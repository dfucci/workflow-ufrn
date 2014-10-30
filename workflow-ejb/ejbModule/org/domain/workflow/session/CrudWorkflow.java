package org.domain.workflow.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.event.ActionEvent;

import org.apache.commons.beanutils.BeanUtils;
import org.domain.dao.UserDAO;
import org.domain.dao.WorkflowDAO;
import org.domain.dataManager.DesignConfigurationManager;
import org.domain.dataManager.WorkflowManager;
import org.domain.dsl.DSLUtil;
import org.domain.exception.ValidationException;
import org.domain.model.User;
import org.domain.model.processDefinition.Artefact;
import org.domain.model.processDefinition.ArtefactFile;
import org.domain.model.processDefinition.DesignType;
import org.domain.model.processDefinition.ProcessDefinition;
import org.domain.model.processDefinition.UserAssignment;
import org.domain.model.processDefinition.Workflow;
import org.domain.utils.ReadPropertiesFile;
import org.domain.workflow.session.generic.CrudAction;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EClassImpl;
import org.eclipse.emf.ecore.impl.EReferenceImpl;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.serial.io.JBossObjectInputStream;
import org.jboss.serial.io.JBossObjectOutputStream;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;
import org.richfaces.model.UploadItem;


@Name("crudWorkflow")
@Restrict("#{identity.loggedIn}")
@Scope(ScopeType.CONVERSATION)
public class CrudWorkflow extends CrudAction<Workflow> {
	
	@In("userDao") UserDAO userDAO;
	@In("workflowDAO") WorkflowDAO workflowDAO;
	
	private User userProperty;
	private String groupProperty;
	private Map<String,List<User>> usersSelectedToShuffle;
	private ProcessDefinition processDefinitionProperty;
	private Artefact currentArtefact;
	private DSLUtil dslUtil;
	private EObject rootModel;
	private TreeNode<EObject> rootNode;
	private TreeNode<EObject> editNode;
	private TreeNode<EObject> selectedNode;
	private EObject newNode;
	
	
	private String textProperty;
	private int intProperty;
	private Enumerator enumProperty;
	private List<Object> refAttrManyProperty;
	
	
	public CrudWorkflow() throws Exception {
		super(Workflow.class);
		this.setUsersSelectedToShuffle(new Hashtable<String,List<User>>());
		dslUtil = DSLUtil.getInstance();
	}
	
	@Override
	public void findEntities()
	{
		setEntities(this.workflowDAO.findAllByUser(user));
	}
	
	@Override
	protected void createImpl(){
		clearEditProperties();
		this.entity.setUser(user);
		this.rootModel = dslUtil.getRootElement();
		this.rootNode = new TreeNodeImpl<EObject>();
		updateTreeNode(null);
		this.selectedNode = this.rootNode.getChild(this.rootModel);
	}
	
	@Override
	protected boolean saveImpl(){
		try {
			this.entity.setDefinition(transform(this.rootModel));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return super.saveImpl();
	}
	
	@Override
	protected void editImpl(){
		try {
			this.rootModel = transform(this.entity.getDefinition());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	};
	
	public EObject transform(byte[] barray) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = null;
		JBossObjectInputStream objIn = null;
		EObject model = null;
		try {
			in = new ByteArrayInputStream(barray);
			objIn = new JBossObjectInputStream(in);
			model = (EObject) objIn.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(objIn != null)
					objIn.close();
				if(in != null)
					in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return model;
	}
	
	public byte[] transform(EObject model) throws IOException {
		ByteArrayOutputStream out = null;
		JBossObjectOutputStream objOut = null;
		try {
			out = new ByteArrayOutputStream();
			objOut = new JBossObjectOutputStream(out);
			objOut.writeObject(model);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(objOut != null)
					objOut.close();
				if(out != null)
					out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return out.toByteArray();
	}
	
	private void updateTreeNode(EObject newNode) {
		TreeNodeImpl<EObject> node = new TreeNodeImpl<EObject>();
		node.setData(this.rootModel);
		rootNode.addChild(this.rootModel, node);
		for (EObject eObject : this.rootModel.eContents()) {
			updateTreeNode(eObject, node, newNode);
		}
	}
	private void updateTreeNode(EObject eObject, TreeNode<EObject> rootNode, EObject newNode) {
		TreeNodeImpl<EObject> node = new TreeNodeImpl<EObject>();
		node.setData(eObject);
		
		if(newNode == eObject) {
			this.selectedNode = node;
		}
		
		rootNode.addChild(eObject, node);
		List<EObject> attrs = dslUtil.getAttrs(eObject);
		for (EObject attr : attrs) {
			TreeNodeImpl<EObject> attrNode = new TreeNodeImpl<EObject>();
			attrNode.setData(attr);
			node.addChild(attr, attrNode);
		}
		
		for (EObject o : eObject.eContents()) {
			updateTreeNode(o, node, newNode);
		}
		
		this.setNewNode(newNode);
	}
	
	public List<TreeNode<EObject>> getChildren(TreeNode<EObject> node) {
		List<TreeNode<EObject>> children = new ArrayList<TreeNode<EObject>>();
		Iterator<Entry<Object, TreeNode<EObject>>> it = node.getChildren();
		while (it.hasNext()) {
			TreeNode<EObject> o = it.next().getValue();
			if(!(o.getData() instanceof EAttribute)) {
				if(o.getData() instanceof EReference) {
					if(((EReference)o.getData()).isContainment()) {
						children.add(o);
					}
				} else {
					children.add(o);
				}
			}
		}
		return children;
	}
	
	public List<EReference>  getRefs(EObject raiz){
		return this.dslUtil.getRefs(raiz);
	} 
	
	@SuppressWarnings("rawtypes")
	public Object getValueFromParent(TreeNode<EObject> node, Boolean toString) {
		Object o = null;
		try {
			EObject e1 = node.getParent().getData();
			EObject e2 = node.getData();
			o = dslUtil.getValue(e1, e2);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		StringBuilder sb = new StringBuilder();
		if(o != null) {
			if(o instanceof EList<?>) {
				for (Object obj : (EList)o) {
					if(obj instanceof EObject) {
						sb.append(getName((EObject) obj)+"\n");
					} else {
						sb.append(obj.toString()+"\n");
					}
				}
			} else {
				sb.append(o.toString()+"\n");
			}
		}
		if(toString){
			return sb.toString();
		} else {
			return o;
		}
	}
	
	public String getFieldType(TreeNode<EObject> node) {
		if(node.getData() instanceof EReference) {
			if(((EReference)node.getData()).isMany()) {
				return "refAttrMany";
			} else {
				return "refAttrOne";
			}
		} else if(node.getData() instanceof EAttribute) {
			if(((EAttribute)node.getData()).getEType().getInstanceTypeName() == "java.lang.String" ){
				return "string";
			} else if(((EAttribute)node.getData()).getEType().getInstanceTypeName() == "int") {
				return "int";
			} else {
				if(((EAttribute)node.getData()).getEType() instanceof EEnum) {
					return "enum";
				}
			}
		}
		return "none";
	}
	
	//passar para o dslutil
	public List<EObject> getFieldValues(EObject eObject){
		return dslUtil.getRefValues(this.rootModel, eObject);
	}
	
	public void updateSelectedNode(TreeNode<EObject> node){
		clearEditProperties();
		this.selectedNode = node;
	}

	@SuppressWarnings("unchecked")
	public void updateEditProperty(TreeNode<EObject> node) {
		try {
			clearEditProperties();
			this.editNode = node;
			Object value = getValueFromParent(node, false);
			
			String type = getFieldType(node);
			
			if(value != null) {
				if(type.equals("refAttrMany") ){
					this.setRefAttrManyProperty((List<Object>)value);
				} else if(type.equals("string") ){
					this.setTextProperty(value.toString());
				} else if(type.equals("int")) {
					this.setIntProperty(Integer.parseInt(value.toString()));
				} else if(type.equals("enum")) {
					this.setEnumProperty((Enumerator) value);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			clearEditProperties();
		}
	}
	public void updateEditProperty() throws Exception {
		try {
			String type = getFieldType(this.editNode);
			EObject refAttr = this.editNode.getData();
			if(type.equals("refAttrMany") ){
				if(refAttr instanceof EStructuralFeature) {
					dslUtil.setValue(this.editNode.getParent().getData(), (EStructuralFeature)refAttr, this.getRefAttrManyProperty());
				}
			} else if(type.equals("string") ){
				if(refAttr instanceof EStructuralFeature) {
					dslUtil.setValue(this.editNode.getParent().getData(), (EStructuralFeature)refAttr, this.getTextProperty());
				} else {
					BeanUtils.setProperty(this.editNode.getParent().getData(), ((EAttribute)this.editNode.getData()).getName(), this.getTextProperty());
				}
			} else if(type.equals("int")) {
				if(refAttr instanceof EStructuralFeature) {
					dslUtil.setValue(this.editNode.getParent().getData(), (EStructuralFeature)refAttr, this.getIntProperty());
				} else {
					BeanUtils.setProperty(this.editNode.getParent().getData(), ((EAttribute)this.editNode.getData()).getName(), this.getIntProperty());
				}
			} else if(type.equals("enum")) {
				if(refAttr instanceof EStructuralFeature) {
					dslUtil.setValue(this.editNode.getParent().getData(), (EStructuralFeature)refAttr, this.getEnumProperty());
				} else {
					BeanUtils.setProperty(this.editNode.getParent().getData(), ((EAttribute)this.editNode.getData()).getName(), this.getEnumProperty());
				}
			}
		} catch (Exception e){
			e.printStackTrace();
			clearEditProperties();
		}
		
		clearEditProperties();
	}

	public void clearEditProperties() {
		this.editNode = null;
		this.setTextProperty("");
		this.setIntProperty(0);
		this.enumProperty = null;
		this.refAttrManyProperty = null;
	}

	public String getItemType(EObject eObject) {
		if(eObject instanceof EAttribute) {
			return "attr";
		} else if (eObject instanceof EReference) {
			if(((EReference)eObject).isContainment()) {
				return "ref";
			} else {
				return "attr";
			}
		}
		return "ref";
	}
	
	public String getName(String s) {
		String className = s;
		String[] r = className.split("(?=\\p{Lu})");
		StringBuilder result = new StringBuilder();
		for (String string : r) {
			result.append(string + " ");
		}
		
		String ret = result.toString();
		if(ret.toLowerCase() == "model") {
			return "Experiment";
		} else {
			return ret;
		}
	}
	
	public String getName(EObject eObject) {
		StringBuilder sb = new StringBuilder();
		if(eObject instanceof EAttribute) {
			sb.append(((EAttribute) eObject).getName());
		} else { 
			boolean lookForName = false;
			if(eObject instanceof EReference && !((EReference)eObject).isContainment()) {
				sb.append(((EReference) eObject).getName());
			} else {
				String className = eObject.getClass().getInterfaces()[0].toString();
				className = className.substring(className.lastIndexOf(".")+1);
				sb.append(className);
				lookForName = true;
			}
			if(lookForName) {
				try {
					String id = BeanUtils.getProperty(eObject, "id");
					if(!id.equals("id") && id != null) {
						sb.append(" [ "+id+" ]");
					}
				} catch (Exception e) {
				}
				try {
					String name = BeanUtils.getProperty(eObject, "name");
					if(!name.equals("name") && name != null) {
						sb.append(" ( "+name+" )");
					}
				} catch (Exception e) {
				}
			}
		}
		
		
		return sb.toString();
	}
	
	public void  buildRef(EObject raiz, EReferenceImpl ref, EClass refClass) {
		try {
			EObject o = this.dslUtil.buildRef(raiz, ref, refClass);
			this.updateTreeNode(o);
		} catch (Exception e) {
			e.printStackTrace();
			getFacesMessages().add(e.getMessage());
		}
	}
	
	//TODO parse jpdl
	public void deployWorkflow(UploadEvent event) throws Exception {
	    UploadItem item = event.getUploadItem();
	    WorkflowManager jpdl = new WorkflowManager(item.getFile().getAbsolutePath(), this.entity, this.seamDao);
	    try {
			jpdl.executeTransformations();
		} catch (ValidationException e) {
			addErrors(e.getErrors());
		} catch (Exception e) {
			throw e;
		}
	}
	
	public String undeployWorkflow() {
		clearDesign();
		for (ProcessDefinition process : entity.getProcessDefinitions()) {
			seamDao.remove(process);
		}
		entity.getProcessDefinitions().clear();
		seamDao.flush();
		seamDao.refresh(entity);
		addInfo("Undeploy efetuado com sucesso");
		return getPage();
	}
	
	public void addUserToShuffle(ActionEvent evt) throws ValidationException{
		if(this.entity.isRCBDDesign()){
			addUserToShuffleRCBD();
		}else if(this.entity.isLSDesign()){
			addUserToShuffleLS();
		}else if (this.entity.isCRDesign()){
			addUserToShuffleCRD();
		}
		this.userProperty = null;
	}
	private void addUserToShuffleCRD() {
		if(this.groupProperty == null)
			this.groupProperty = "Subjects";
		addUserToShuffleBlock();
	}
	private void addUserToShuffleRCBD(){
		addUserToShuffleBlock();
	}
	private void addUserToShuffleLS(){
		addUserToShuffleBlock();
	}
	private void addUserToShuffleBlock() {
		if(this.userProperty != null && this.groupProperty != null && !isUserPresentToShuffle(this.userProperty)){
			if(!this.getUsersSelectedToShuffle().containsKey(this.groupProperty)){
				this.getUsersSelectedToShuffle().put(this.groupProperty, new ArrayList<User>());
			}
			this.getUsersSelectedToShuffle().get(this.groupProperty).add(this.userProperty);
		}
	}
	private boolean isUserPresentToShuffle(User user) {
		for (List<User> users : this.getUsersSelectedToShuffle().values()) {
			for (User u : users) {
				if(user.equals(u)){
					return true;
				}
			}
		}
		return false;
	}

	public void removeUserToSuffle(User u, String group){
		this.usersSelectedToShuffle.get(group).remove(u);
	}
	public List<String> getGroupValues(){
//		List<String> groups = new ArrayList<String>();
//		for (String string : this.getUsersSelectedToShuffle().keySet()) {
//			groups.add(string);
//		}
//		return groups;
		if(entity.isCRDesign()){
			ArrayList<String> r = new ArrayList<String>();
			r.add("Subjects");
			return r;
		}
		return new ArrayList<String>(entity.getGroups());
	}
	public void suffleUsersBlock() throws ValidationException{
		boolean hasError = false;
		for (String groupValue : getGroupValues()) {
			if (this.getUsersSelectedToShuffle().get(groupValue) == null){
				getFacesMessages().add("Incomplete configuration");
				hasError = true;
			} else if(this.getUsersSelectedToShuffle().get(groupValue).size() < this.entity.getQuantityOfSubjectsNeeds(groupValue)){
				getFacesMessages().add("User quantity is not enought for the group "+groupValue);
				hasError = true;
			}
		}
		if(!hasError){
			for (String groupValue : getGroupValues()) {
				List<User> users = this.getUsersSelectedToShuffle().get(groupValue);
				Collections.shuffle(users);
				for (User user : users) {
					this.entity.addUserToGroup(groupValue, user);
				}
			}
		}
		
		seamDao.merge(this.entity);
		seamDao.flush();
	}
	public void shuffleUsersRCDB() throws ValidationException{
		suffleUsersBlock();
	}
	public void shuffleUsersLS() throws ValidationException{
		boolean hasError = false;
		for (String groupValue : getGroupValues()) {
			if(this.getUsersSelectedToShuffle().get(groupValue) == null || this.getUsersSelectedToShuffle().get(groupValue).size() < this.entity.getQuantityOfSubjectsNeeds(groupValue)){
				getFacesMessages().add("User quantity is not enought for the group "+groupValue);
				hasError = true;
			}
		}
		if(!hasError){
			for (String groupValue : getGroupValues()) {
				List<User> users = this.getUsersSelectedToShuffle().get(groupValue);
				Collections.shuffle(users);
				for (User user : users) {
					this.entity.addUserToGroup(groupValue, user);
				}
			}
		}
		
		seamDao.merge(this.entity);
		seamDao.flush();
	}
	public void shuffleUsersCRD() throws ValidationException{
		suffleUsersBlock();
	}
	
	public void addUserManual(ActionEvent evt) throws ValidationException{
		seamDao.refresh(entity);
		if(userProperty != null && getProcessDefinitionProperty() != null && !getProcessDefinitionProperty().getUsers().contains(userProperty)
				&& entity.isManualDesign() ){
			UserAssignment userAssignment = new UserAssignment(userProperty, getProcessDefinitionProperty());
			seamDao.persist(userAssignment);
			seamDao.refresh(getProcessDefinitionProperty());

			getProcessDefinitionProperty().getUserAssignments().add(userAssignment);
			seamDao.merge(getProcessDefinitionProperty());
			
			seamDao.flush();
			userProperty = null;
			processDefinitionProperty = null;
		}
	}
	
	public void removeUserManual(UserAssignment userAssignment) throws ValidationException{
		if(entity.isManualDesign()  ){
			seamDao.refresh(userAssignment);
			seamDao.remove(userAssignment);
			
			processDefinitionProperty = userAssignment.getProcessDefinition();
			userProperty = userAssignment.getUser();
			
			processDefinitionProperty.getUserAssignments().remove(userAssignment);
			seamDao.merge(processDefinitionProperty);
			
			seamDao.flush();
		}
	}
	
	public void start(Workflow w) throws ValidationException{
		seamDao.refresh(w);
		w.setStartedAt(Calendar.getInstance().getTime());
		w.nextTurn();
		seamDao.merge(w);
		seamDao.flush();
	}
	public void nextTurn(Workflow w) throws ValidationException{
		seamDao.refresh(w);
		w.nextTurn();
		seamDao.merge(w);
		seamDao.flush();
	}
	
	public void updateDesignTypeToManual(){
		try {
			seamDao.refresh(entity);
			this.entity.setDesignType(DesignType.MANUAL);
			seamDao.merge(entity);
			seamDao.flush();
		} catch (ValidationException e) {
			getFacesMessages().add("Validation error.");
		}
	}
	public void clearDesign(){
		this.usersSelectedToShuffle.clear();
		try {
			seamDao.refresh(entity);
			this.entity.setCurrentTurn(null);
			this.entity.setTurnQuantity(null);
			this.entity.setDesignType(null);
			for (ProcessDefinition p : entity.getProcessDefinitions()) {
				for (UserAssignment ua : p.getUserAssignments()) {
					seamDao.remove(ua);
				}
				p.getUserAssignments().clear();
			}
			seamDao.merge(entity);
			seamDao.flush();
		} catch (ValidationException e) {
			getFacesMessages().add("Validation error.");
		}
	}
	
	public void setCurrentArtefact(Artefact artefact){
		this.currentArtefact = artefact;
	}
	public void uploadArtefact(UploadEvent event) throws Exception {
		ArtefactFile artefactfile = new ArtefactFile();
		seamDao.persist(artefactfile);
		
		String path = ReadPropertiesFile.getProperty("components", "artefactPath");
		path = path + this.currentArtefact.getId() + "/" + artefactfile.getId() + "/";
		File upload = new File(path);
		upload.mkdirs();
		
		path = path + event.getUploadItem().getFileName();
	    if(event.getUploadItem().getFile().renameTo(new File(path))){
	    	artefactfile.setFile(path);		
	    	artefactfile.setArtefact(this.currentArtefact);
	    	seamDao.merge(artefactfile);
	    	this.currentArtefact.getArtefactFiles().add(artefactfile);
	    	seamDao.merge(this.currentArtefact);
	    	seamDao.flush();
	    }
	}
	public void uploadDesignConfiguration(UploadEvent event) {
		try{
			seamDao.refresh(this.entity);
			UploadItem item = event.getUploadItem();
			DesignConfigurationManager design = new DesignConfigurationManager(item.getFile().getAbsolutePath(), this.entity);
			this.entity = design.executeTransformations(this.entity);
		
			for (UserAssignment ua : this.entity.getAllUserAssignments()) {
				seamDao.persist(ua);
			}
			seamDao.merge(this.entity);
		} catch(Exception e){
			getFacesMessages().add("Erro ao importar design");
		}
	}
	
	public void removeArtefact(Artefact artefact) throws Exception {
		List<ArtefactFile> artefactsFiles = artefact.getArtefactFiles();
		for (ArtefactFile artefactFile : artefactsFiles) {
			seamDao.remove(artefactFile);
		}
		artefact.getArtefactFiles().clear();
		seamDao.merge(artefact);
		seamDao.flush();
	}
	public List<User> getUsers(){
		return userDAO.findAll(User.class);
	}
	
	public User getUserProperty() {
		return userProperty;
	}

	public void setUserProperty(User userProperty) {
		this.userProperty = userProperty;
	}

	@Override
	protected Workflow getExampleForFind() {
		return new Workflow();
	}

	public ProcessDefinition getProcessDefinitionProperty() {
		return processDefinitionProperty;
	}

	public void setProcessDefinitionProperty(ProcessDefinition processDefinitionProperty) {
		this.processDefinitionProperty = processDefinitionProperty;
	}

	public String getGroupProperty() {
		return groupProperty;
	}

	public void setGroupProperty(String groupProperty) {
		this.groupProperty = groupProperty;
	}

	public Map<String,List<User>> getUsersSelectedToShuffle() {
		return usersSelectedToShuffle;
	}

	public void setUsersSelectedToShuffle(Map<String,List<User>> usersSelectedToShuffle) {
		this.usersSelectedToShuffle = usersSelectedToShuffle;
	}

	public DSLUtil getDslUtil() {
		return dslUtil;
	}

	public void setDslUtil(DSLUtil dslUtil) {
		this.dslUtil = dslUtil;
	}

	public TreeNode<EObject> getRootNode() {
		return rootNode;
	}

	public void setRootNode(TreeNode<EObject> rootNode) {
		this.rootNode = rootNode;
	}

	public String getTextProperty() {
		return textProperty;
	}

	public void setTextProperty(String textProperty) {
		this.textProperty = textProperty;
	}

	public TreeNode<EObject> getEditNode() {
		return editNode;
	}

	public void setEditNode(TreeNode<EObject> editNode) {
		this.editNode = editNode;
	}
	
	public void doNothing(){
		System.err.println("do nothing...");
	}

	public int getIntProperty() {
		return intProperty;
	}

	public void setIntProperty(int intProperty) {
		this.intProperty = intProperty;
	}

	public Enumerator getEnumProperty() {
		return enumProperty;
	}

	public void setEnumProperty(Enumerator enumProperty) {
		this.enumProperty = enumProperty;
	}

	public EObject getNewNode() {
		return newNode;
	}

	public void setNewNode(EObject newNode) {
		this.newNode = newNode;
	}

	public List<Object> getRefAttrManyProperty() {
		return refAttrManyProperty;
	}

	public void setRefAttrManyProperty(List<Object> refAttrManyProperty) {
		this.refAttrManyProperty = refAttrManyProperty;
	}

	public TreeNode<EObject> getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(TreeNode<EObject> selectedNode) {
		this.selectedNode = selectedNode;
	}

}
