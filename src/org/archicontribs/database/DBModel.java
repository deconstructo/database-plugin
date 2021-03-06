/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.archicontribs.database.DBPlugin.DebugLevel;
import org.archicontribs.database.DBPlugin.Level;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.canvas.model.ICanvasModelBlock;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelGroup;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IMetadata;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.ISketchModelSticky;


/**
 * The DBModel class is a proxy to the IArchimateModel class in standalone mode, and to the IFolder class in shared mode.
 * <br>
 * It allows transparent access methods of the right object  
 * 
 * @author Herv�
 *
 */
public class DBModel {
	/**
	 * The IArchimate object:
	 * <br>
	 * <li>In standalone mode : the whole model</li>
	 * <li>In shared mode : the container model</li>
	 */
	private IArchimateModel model = null;

	/**
	 * <li>In standalone mode : null</li>
	 * <li>In shared mode : the project folder (as a subfolder of the folder "Models")</li>
	 */
	private IFolder projectFolder = null;

	/**
	 * Name of the model (standalone mode) or project (shared mode) owner
	 */
	String owner = null;

	private int nbElements = 0;
	private Hashtable<String, EObject> allElements = null;
	private int nbRelationships = 0;
	private Hashtable<String, EObject> allRelationships = null;
	private int nbArchimateDiagramModels = 0;
	private Hashtable<String, EObject> allArchimateDiagramModels = null;
	private int nbCanvasModels = 0;
	private Hashtable<String, EObject> allCanvasModels = null;
	private int nbCanvasModelBlocks = 0;
	private Hashtable<String, EObject> allCanvasModelBlocks = null;
	private int nbCanvasModelConnections = 0;
	private Hashtable<String, EObject> allCanvasModelConnections = null;
	private int nbCanvasModelImages = 0;
	private Hashtable<String, EObject> allCanvasModelImages = null;
	private int nbCanvasModelStickys = 0;
	private Hashtable<String, EObject> allCanvasModelStickys = null;
	private int nbDiagramModelArchimateConnections = 0;
	private Hashtable<String, EObject> allDiagramModelArchimateConnections = null;
	private int nbDiagramModelArchimateObjects = 0;
	private Hashtable<String, EObject> allDiagramModelArchimateObjects = null;
	private int nbDiagramModelConnections = 0;
	private Hashtable<String, EObject> allDiagramModelConnections = null;
	private int nbDiagramModelReferences = 0;
	private Hashtable<String, EObject> allDiagramModelReferences = null;
	private int nbDiagramModelGroups = 0;
	private Hashtable<String, EObject> allDiagramModelGroups = null;
	private int nbDiagramModelNotes = 0;
	private Hashtable<String, EObject> allDiagramModelNotes = null;
	private int nbFolders = 0;
	private Hashtable<String, EObject> allFolders = null;
	private int nbSketchModels = 0;
	private Hashtable<String, EObject> allSketchModels = null;
	private int nbSketchModelActors = 0;
	private Hashtable<String, EObject> allSketchModelActors = null;
	private int nbSketchModelStickys = 0;
	private Hashtable<String, EObject> allSketchModelStickys = null;
	private int nbImagePaths = 0;
	private Set<String> allImagePaths = null;
	private int nbDiagramModelBendpoints = 0;
	private int nbMetadatas = 0;
	private int nbProperties = 0;

	private List<Hashtable<String, EObject>> allContent = Arrays.asList(allElements,
			allRelationships,
			allArchimateDiagramModels,
			allCanvasModels,
			allCanvasModelBlocks,
			allCanvasModelConnections,
			allCanvasModelImages,
			allCanvasModelStickys,
			allDiagramModelArchimateConnections,
			allDiagramModelArchimateObjects,
			allDiagramModelConnections,
			allDiagramModelReferences,
			allDiagramModelGroups,
			allDiagramModelNotes,
			allFolders,
			allSketchModels,
			allSketchModelActors,
			allSketchModelStickys);

	/**
	 * Table containing an Arraylist of children. Contains one entry per parent. 
	 */
	private Hashtable<String, ArrayList<EObject>> family = new Hashtable<String, ArrayList<EObject>>();
	/**
	 * Table containing an Arraylist of sourceConnections. Contains one entry per parent. 
	 */
	private Hashtable<String, ArrayList<EObject>> sourceConnections = new Hashtable<String, ArrayList<EObject>>();

	public DBModel() {
		this(null, null);
	}
	public DBModel(IArchimateModel _model) {
		this(_model, null);
	}
	public DBModel(IArchimateModel _model, IFolder _folder) {
		DBPlugin.debug(DebugLevel.MainMethod, "new DBModel("+(_model==null?"null":_model.getName())+","+(_folder==null?"null":_folder.getName())+")");
		projectFolder = _folder;
		
		if ( _model == null ) {
			// If no existing model is provided, then we set it to the shared container
			for (IArchimateModel m: IEditorModelManager.INSTANCE.getModels() ) {
				if ( m.getId().equals(DBPlugin.SharedModelId) ) {
					model = m;
					return;
				}
			}
			// if the shared container doesn't exist, we create it 
			if ( model == null ) {
				model = IArchimateFactory.eINSTANCE.createArchimateModel();
				model.setDefaults();
				model.setId(DBPlugin.SharedModelId);
				model.setName("Shared container");
				model.setPurpose("This model is a container for all the models imported in shared mode.");
				IEditorModelManager.INSTANCE.registerModel(model);
			}
		} else {
			model = _model;
		}
	}

	/**
	 * returns the model. In shared mode, the container model is returned.
	 * 
	 * @return IArchimateModel
	 */
	public IArchimateModel getModel() {
		return model;
	}

	/**
	 * returns the folder in shared mode.
	 * 
	 * @return
	 */
	public IFolder getProjectFolder() {
		return projectFolder;
	}

	/**
	 * Sets the project folder in shared mode.
	 * 
	 * @param _folder
	 * @return IFolder
	 */
	public IFolder setProjectFolder(IFolder _folder) {
		return projectFolder=_folder;
	}

	/**
	 * Create the project folders structure : one project subfolder per first-level model folder (Business, Application, ...)
	 * 
	 * @param _projectId
	 * @param _version
	 * @param _name
	 * @param _purpose
	 * @return IFolder
	 */
	public IFolder addFolder(String _projectId, String _version, String _name, String _purpose) {
		boolean sharedFolderExists = false;

		for ( IFolder f: model.getFolders() ) {
			// we create a sub-folder in all first-level model folders
			boolean exists = false;
			for ( IFolder ff: f.getFolders() ) {
				if ( DBPlugin.getProjectId(ff.getId()).equals(_projectId) ) {
					exists = true;	// the subfolder already exists, we do not need to create it
					break;
				}
			}
			if ( !exists ) {
				IFolder subFolder = addFolder(f.getFolders(), _name, _projectId, _version);
				if ( f.getName().equals(DBPlugin.SharedFolderName) ) {
					subFolder.setDocumentation(_purpose);
					sharedFolderExists = true;
					projectFolder = subFolder;
				}
			}
		}
		if ( !sharedFolderExists ) {
			IFolder sharedModelsFolder = addFolder(model.getFolders(), DBPlugin.SharedFolderName);
			projectFolder = addFolder(sharedModelsFolder.getFolders(), _name, _projectId, _version);
			projectFolder.setDocumentation(_purpose);
		}
		return projectFolder;
	}
	
	private IFolder addFolder(EList<IFolder> _parentFolder, String _name) {
		IFolder result = addFolder(_parentFolder, _name, null, null, -1);
		return result;
	}
	private IFolder addFolder(EList<IFolder> _parentFolder, String _name, String _projectId, String _version) {
		IFolder result = addFolder(_parentFolder, _name, _projectId, _version, -1);
		return result;
	}
	private IFolder addFolder(EList<IFolder> _parentFolder, String _name, String _projectId, String _version, int _position) {
		IFolder subFolder = IArchimateFactory.eINSTANCE.createFolder();
		subFolder.setId(DBPlugin.generateId(null, _projectId, _version));
		subFolder.setName(_name);
		if ( _position == -1 )
			_parentFolder.add(subFolder);
		else
			_parentFolder.add(_position, subFolder);
		return subFolder;
	}

	/**
	 * Determines if the model's ID (standalone mode) or project folder's ID (shared mode) contains a version number.
	 * @return true of false
	 */
	public boolean isVersionned() {
		boolean result = DBPlugin.isVersionned(projectFolder == null ? model.getId() : projectFolder.getId());
		return result;
	}

	/**
	 * Decodes the project's ID from the ID of the model (standalone mode) or the project folder (shared mode)
	 * 
	 * @return
	 */
	public String getProjectId() {
		String result = DBPlugin.getProjectId(projectFolder!=null ? projectFolder.getId() : model.getId());
		return result;
	}

	/**
	 * Encodes the project ID and the version in all model (standalone mode) or project (shared mode) elements.
	 * @param _projectId
	 * @param _version
	 * @return
	 */
	public void setProjectId(String _projectId, String _version) {
		// in standalone mode, we change the model ID, which is not necessary in shared mode
		if ( projectFolder == null )
			model.setId(DBPlugin.generateProjectId(_projectId, _version));
		else
			projectFolder.setId(DBPlugin.generateId(DBPlugin.getId(projectFolder.getId()), _projectId, _version));
	}

	/**
	 * Decodes the version from the ID of the model (standalone mode) or the project folder (shared mode)
	 * 
	 * @return
	 */
	public String getVersion() {
		return DBPlugin.getVersion(projectFolder!=null ? projectFolder.getId() : model.getId());
	}

	/**
	 * gets the properties of the model (standalone mode) or the project folder (shared mode)
	 * @return
	 */
	public EList<IProperty> getProperties() {
		return projectFolder!=null ? projectFolder.getProperties() : model.getProperties();
	}

	/**
	 * gets the metadata of the model (standalone mode) or null (shared mode)
	 * @return
	 */
	public EList<IProperty> getMetadata() {
		// In standalone mode, we return the model's metadata
		if (projectFolder == null ) {
			if ( model.getMetadata() == null ) {
				model.setMetadata(IArchimateFactory.eINSTANCE.createMetadata());
			}
			return model.getMetadata().getEntries();
		}

		// In shared mode, we return the project's metadata folder properties
		for ( IFolder f: getProjectFolder().getFolders() ) {
			if ( f.getName().equals("Metadata") ) {
				return f.getProperties();
			}
		}
		IFolder metadata = IArchimateFactory.eINSTANCE.createFolder();
		metadata.setId(DBPlugin.generateId(null, getProjectId(), getVersion()));
		metadata.setName("Metadata");
		getProjectFolder().getFolders().add(metadata);
		return metadata.getProperties();
	}

	/**
	 * Gets the name of the model (standalone mode) or the project folder (shared mode)
	 * @return
	 */
	public String getName() {
		return projectFolder!=null ? projectFolder.getName() : model.getName();
	}

	/**
	 * Sets the name of the model (standalone mode) or the project folder (shared mode)
	 * @param _name
	 * @return
	 */
	public String setName(String _name) {
		if ( _name == null || _name.trim().isEmpty() )
			return null;
		if ( projectFolder == null )
			model.setName(_name);
		else for ( IFolder f: model.getFolders() ) {
			// we create a sub-folder in all the model's folders (if it does not exist yet)
			for ( IFolder ff: f.getFolders() ) {
				if ( DBPlugin.getProjectId(projectFolder.getId()).equals(DBPlugin.getProjectId(ff.getId())) ) {	// we do not check the version as we can only have one version at a time
					ff.setName(_name);
				}
			}
		}
		return _name;
	}

	/**
	 * Gets the purpose of the model (standalone mode) or thedocumentation of the project folder (shared mode)
	 * @return
	 */
	public String getPurpose() {
		return projectFolder!= null ? projectFolder.getDocumentation() : model.getPurpose();
	}

	/**
	 * Sets the purpose of the model (standalone mode) or the documentation of the project folder (shared mode)
	 * @param _purpose
	 */
	public void setPurpose(String _purpose) {
		if ( _purpose == null )
			_purpose = "";

		if ( projectFolder!= null )
			projectFolder.setDocumentation(_purpose);
		else
			model.setPurpose(_purpose);
	}

	/**
	 * gets the owner's name
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * sets the owner's name
	 */
	public void setOwner(String _owner) {
		owner = _owner;
	}


	/**
	 * Gets the subfolders of the model (standalone mode) or the project folder (shared mode)
	 * @return
	 */
	public EList<IFolder> getFolders() {
		if ( projectFolder == null )
			return model.getFolders();

		EList<IFolder> folders = new BasicEList<IFolder>();
		for ( IFolder f: model.getFolders() ) {
			for ( IFolder ff: f.getFolders() ) {
				if ( (DBPlugin.isVersionned(ff.getId()) && DBPlugin.getProjectId(ff.getId()).equals(DBPlugin.getProjectId(projectFolder.getId()))) || ff.getName().equals(projectFolder.getName()) )
					folders.add(ff);
			}
		}
		return folders;
	}

	/**
	 * Gets a list of projects folders (shared mode only).
	 * @return
	 */
	public EList<IFolder> getProjectsFolders() {
		for ( IFolder f: model.getFolders() ) {
			if ( f.getName().equals(DBPlugin.SharedFolderName) ) return f.getFolders();
		}
		return null; //TODO : in standalone mode, return a list containing the current model
	}

	/**
	 * Gets the default folder for a given folder type.
	 * @param _folderType
	 * @return
	 * <li>In standalone mode, the standard "Business", "Application", ... folder</li>
	 * <li>In shared mode, the project subfolder of "Business", "Application", ...</li>
	 */
	public IFolder getDefaultFolderForFolderType(FolderType _folderType ) {
		IFolder parentFolder = null;

		for ( IFolder f: model.getFolders() ) {
			if ( f.getType().equals(_folderType) ) {
				parentFolder = f;
				if ( projectFolder == null )
					return f;									// in standalone mode, we return the model's folder
				for ( IFolder ff: f.getFolders() ) {			// in shared mode, we look for the sub-folder that has got the correct ID
					if ( DBPlugin.getProjectId(ff.getId()).equals(DBPlugin.getProjectId(projectFolder.getId())) )
						return ff;
				}
			}
		}
		// if the folder does not exist, it MUST be created !!!
		if ( parentFolder == null ) {
			parentFolder = addFolder(model.getFolders(), _folderType.getName(), null, null, _folderType.getValue());
		}
		// in shared mode, we create a subfolder with the name of the project
		if ( projectFolder != null ) {
			return addFolder(parentFolder.getFolders(), projectFolder.getName(), DBPlugin.getProjectId(projectFolder.getId()), DBPlugin.getVersion(projectFolder.getId()));
		}
		return parentFolder;
	}

	/**
	 *  Gets the default folder for a given element
	 * @param _eObject
	 * @return
	 * <li>In standalone mode, the standard "Business", "Application", ... folder</li>
	 * <li>In shared mode, the project subfolder of "Business", "Application", ...</li>
	 */
	public IFolder getDefaultFolderForElement(EObject _eObject) {
		//WARNING: DERIVED folder is not return by getDefaultFolderForElement method
		if ( projectFolder == null )
			return model.getDefaultFolderForObject(_eObject);
		for ( IFolder f: model.getDefaultFolderForObject(_eObject).getFolders() ) {
			if ( DBPlugin.getProjectId(f.getId()).equals(DBPlugin.getProjectId(((IIdentifier)_eObject).getId())) ) {
				return f;
			}
		}
		return null;	//shouldn't be the case, but we never know ...
	}

	/**
	 * Determines if we are in standalone or shared mode.
	 * @return
	 * <li>false : standalone mode</li>
	 * <li>true : shared mode</li>
	 */
	public boolean isShared() {
		return model.getId().equals(DBPlugin.SharedModelId);
	}

	/**
	 * Puts the existing EObject of the entire model (standalone mode) or of the project folders (shared mode) in hashtables to count them and allow future retrieval
	 * @param _element
	 */
	public void initializeIndex() {
		allElements = new Hashtable<String, EObject>();
		allRelationships = new Hashtable<String, EObject>();
		allArchimateDiagramModels = new Hashtable<String, EObject>();
		allCanvasModels = new Hashtable<String, EObject>();
		allCanvasModelBlocks = new Hashtable<String, EObject>();
		allCanvasModelConnections = new Hashtable<String, EObject>();
		allCanvasModelImages = new Hashtable<String, EObject>();
		allCanvasModelStickys = new Hashtable<String, EObject>();
		allDiagramModelArchimateConnections = new Hashtable<String, EObject>();
		allDiagramModelArchimateObjects = new Hashtable<String, EObject>();
		allDiagramModelConnections = new Hashtable<String, EObject>();
		allDiagramModelReferences = new Hashtable<String, EObject>();
		allDiagramModelGroups = new Hashtable<String, EObject>();
		allDiagramModelNotes = new Hashtable<String, EObject>();
		allFolders = new Hashtable<String, EObject>();
		allSketchModels = new Hashtable<String, EObject>();
		allSketchModelActors = new Hashtable<String, EObject>();
		allSketchModelStickys = new Hashtable<String, EObject>();
		allImagePaths=new HashSet<String>();
		nbDiagramModelBendpoints = 0;
		nbMetadatas = 0;
		nbProperties=0;
		allContent = Arrays.asList(allElements,
				allRelationships,
				allArchimateDiagramModels,
				allCanvasModels,
				allCanvasModelBlocks,
				allCanvasModelConnections,
				allCanvasModelImages,
				allCanvasModelStickys,
				allDiagramModelArchimateConnections,
				allDiagramModelArchimateObjects,
				allDiagramModelConnections,
				allDiagramModelReferences,
				allDiagramModelGroups,
				allDiagramModelNotes,
				allFolders,
				allSketchModels,
				allSketchModelActors,
				allSketchModelStickys);
		
		// we populate the hashtables with existing components
		if ( projectFolder == null ) {
			for(Iterator<EObject> iter = model.eAllContents(); iter.hasNext();)
				indexEObject(iter.next());
		} else {
			for ( IFolder f: getFolders() ) {
				for(Iterator<EObject> iter = f.eAllContents(); iter.hasNext();)
					indexEObject(iter.next());
			}
		}
		
		//we must load only new images, not those already loaded 
		//IArchiveManager archiveMgr = (IArchiveManager)model.getAdapter(IArchiveManager.class);
		//for ( String path: archiveMgr.getImagePaths() ) {
		//	allImagePaths.add(path);
		//}
		
		return;
	}
	
	/**
	 * Puts the EObject in a hashtable to allow future retrieval
	 * @param _element
	 */
	public void indexEObject(EObject _obj) {
		//we change the object projectID and version if necessary (and if the component has got an ID of course)
		try {
			if ( !DBPlugin.getProjectId(((IIdentifier)_obj).getId()).equals(getProjectId()) || !DBPlugin.getVersion(((IIdentifier)_obj).getId()).equals(getVersion()) ) {
				((IIdentifier)_obj).setId(DBPlugin.generateId(DBPlugin.getId(((IIdentifier)_obj).getId()), getProjectId(), getVersion()));
			}
		} catch ( ClassCastException e) {}

		switch ( _obj.eClass().getName() ) {
		case "ArchimateDiagramModel" :			allArchimateDiagramModels.put(((IIdentifier)_obj).getId(), _obj); break;
		case "Bounds" :							break;
		case "CanvasModel" :					allCanvasModels.put(((IIdentifier)_obj).getId(), _obj); break;
		case "CanvasModelBlock" :				allCanvasModelBlocks.put(((IIdentifier)_obj).getId(), _obj); break;
		case "CanvasModelConnection" :			allCanvasModelConnections.put(((IIdentifier)_obj).getId(), _obj); break;
		case "CanvasModelImage" :				allCanvasModelImages.put(((IIdentifier)_obj).getId(), _obj); break;
		case "CanvasModelSticky" :				allCanvasModelStickys.put(((IIdentifier)_obj).getId(), _obj); break;
		case "DiagramModelArchimateConnection": allDiagramModelArchimateConnections.put(((IIdentifier)_obj).getId(), _obj); break;
		case "DiagramModelArchimateObject" :	allDiagramModelArchimateObjects.put(((IIdentifier)_obj).getId(), _obj); break;
		case "DiagramModelBendpoint" :			++nbDiagramModelBendpoints; break ;
		case "DiagramModelConnection" :			allDiagramModelConnections.put(((IIdentifier)_obj).getId(), _obj); break;
		case "DiagramModelReference" :			allDiagramModelReferences.put(((IIdentifier)_obj).getId(), _obj); break;
		case "DiagramModelGroup" :				allDiagramModelGroups.put(((IIdentifier)_obj).getId(), _obj); break;
		case "DiagramModelNote" :				allDiagramModelNotes.put(((IIdentifier)_obj).getId(), _obj); break;
		case "Folder" :							allFolders.put(((IIdentifier)_obj).getId(), _obj); break;
		case "Property" :						++nbProperties; break;
		case "SketchModel" :					allSketchModels.put(((IIdentifier)_obj).getId(), _obj); break;
		case "SketchModelActor" :				allSketchModelActors.put(((IIdentifier)_obj).getId(), _obj); break;
		case "SketchModelSticky" :				allSketchModelStickys.put(((IIdentifier)_obj).getId(), _obj); break;
		case "Metadata":						nbMetadatas++; --nbProperties; break;
		default :
			// here, the class is too detailed (Node, Artefact, BusinessActor, etc ...)
			// so we use "instanceof" to distinguish elements from relationships
			if ( _obj instanceof IArchimateElement ) {
				allElements.put(((IIdentifier)_obj).getId(), _obj);
			} else if ( _obj instanceof IArchimateRelationship ) {
				allRelationships.put(((IIdentifier)_obj).getId(), _obj);						
			} else {
				//we shouldn't be there but just in case
				DBPlugin.popup(Level.Error, "DBModel : I do not know how to index " + _obj.eClass().getName() + " components !!!");
			}
		}
	}
	
	public void indexMetadata() {
		nbMetadatas++; --nbProperties;
	}
	
	public void indexImagePath(String _path) {
		if ( _path != null )
			allImagePaths.add(_path);
	}

	public void countExistingEObjects() {
		nbElements = 0;
		nbRelationships = 0;
		nbArchimateDiagramModels = 0;
		nbCanvasModels = 0;
		nbCanvasModelBlocks = 0;
		nbCanvasModelConnections = 0;
		nbCanvasModelImages = 0;
		nbCanvasModelStickys = 0;
		nbDiagramModelArchimateConnections = 0;
		nbDiagramModelArchimateObjects = 0;
		nbDiagramModelConnections = 0;
		nbDiagramModelReferences = 0;
		nbDiagramModelGroups = 0;
		nbDiagramModelNotes = 0;
		nbFolders = 0;
		nbSketchModels = 0;
		nbSketchModelActors = 0;
		nbSketchModelStickys = 0;
		nbImagePaths = 0;
		nbDiagramModelBendpoints = 0;
		nbMetadatas = 0;
		nbProperties=0;

		if ( projectFolder == null ) {
			for(Iterator<EObject> iter = model.eAllContents(); iter.hasNext();)
				countEObject(iter.next());
		} else {
			for ( IFolder f: getFolders() ) {
				// in shared mode, we do not save the count the project folder itself ... but we count its properties
				if ( (getProjectFolder() == null) || !f.getId().equals(getProjectFolder().getId()) ) {
					countEObject(f);
					for(Iterator<EObject> iter = f.eAllContents(); iter.hasNext();)
						countEObject(iter.next());
				}
			}
		}
		
		IArchiveManager archiveMgr = (IArchiveManager)model.getAdapter(IArchiveManager.class);
		for ( String path: archiveMgr.getImagePaths() ) {
			countImagePath(path);
		}
		
		// in sharedmode, we count the metadata manually because they are stored in a "Metadata" folder properties and we count the project properties
		if ( projectFolder != null ) {
			nbProperties += getProjectFolder().getProperties().size();
			nbMetadatas += getMetadata().size();
		}
	}

	public void countEObject(EObject _obj) {
		//we change the object projectID and version if necessary (and if the component has got an ID of course)
		try {
			if ( !DBPlugin.getProjectId(((IIdentifier)_obj).getId()).equals(getProjectId()) || !DBPlugin.getVersion(((IIdentifier)_obj).getId()).equals(getVersion()) ) {
				((IIdentifier)_obj).setId(DBPlugin.generateId(DBPlugin.getId(((IIdentifier)_obj).getId()), getProjectId(), getVersion()));
			}
		} catch ( ClassCastException e) {}

		switch ( _obj.eClass().getName() ) {
		case "ArchimateDiagramModel" :			++nbArchimateDiagramModels; break;
		case "Bounds" :							break;
		case "CanvasModel" :					++nbCanvasModels; break;
		case "CanvasModelBlock" :				++nbCanvasModelBlocks; break;
		case "CanvasModelConnection" :			++nbCanvasModelConnections; break;
		case "CanvasModelImage" :				++nbCanvasModelImages; break;
		case "CanvasModelSticky" :				++nbCanvasModelStickys; break;
		case "DiagramModelArchimateConnection": ++nbDiagramModelArchimateConnections; break;
		case "DiagramModelArchimateObject" :	++nbDiagramModelArchimateObjects; break;
		case "DiagramModelBendpoint" :			++nbDiagramModelBendpoints; break ;
		case "DiagramModelConnection" :			++nbDiagramModelConnections; break;
		case "DiagramModelReference" :			++nbDiagramModelReferences; break;
		case "DiagramModelGroup" :				++nbDiagramModelGroups; break;
		case "DiagramModelNote" :				++nbDiagramModelNotes; break;
		case "Folder" :							++nbFolders; break;
		case "Property" :						++nbProperties; break;
		case "SketchModel" :					++nbSketchModels; break;
		case "SketchModelActor" :				++nbSketchModelActors; break;
		case "SketchModelSticky" :				++nbSketchModelStickys; break;
		case "Metadata":						nbMetadatas+=((IMetadata)_obj).getEntries().size() ; nbProperties-=((IMetadata)_obj).getEntries().size(); break;
		default :
			// here, the class is too detailed (Node, Artefact, BusinessActor, etc ...)
			// so we use "instanceof" to distinguish elements from relationships
			if ( _obj instanceof IArchimateElement ) {
				++nbElements;
			} else if ( _obj instanceof IArchimateRelationship ) {
				++nbRelationships;						
			} else {
				//we shouldn't be there but just in case
				DBPlugin.popup(Level.Error, "DBModel : I do not know how to count " + _obj.eClass().getName() + " components !!!");
			}
		}
	}
	
	public void countImagePath(String _path) {
		++nbImagePaths;
	}

	/**
	 * Retrieve an EObject from the its ID
	 * @return EObject
	 */
	public EObject searchEObjectById(String _id) {
		EObject obj;

		for (Hashtable<String, EObject> content: allContent) {
			if ( content != null ) {
				obj = content.get(_id);
				if ( obj != null ) return obj;
			}
		}
		return null;
	}

	/**
	 * Search a folder by ID.
	 *  
	 * @param _id
	 * @return IFolder
	 */
	public IFolder searchFolderById(String _id) {
		if ( allFolders != null )
			return (IFolder)allFolders.get(_id);
		
		return searchFolderById(model.getFolders(), _id);
	}
	
	public void setFolder(String parentId, EObject object) throws Exception {
		if ( !DBPlugin.isVersionned(parentId) )
			parentId = DBPlugin.generateId(parentId, getProjectId(), getVersion());
				
		IFolder parent = searchFolderById(getFolders(), parentId);
		
		if ( parent == null )
			throw new Exception("Cannot find folder (id = " + parentId);
		
		parent.getElements().add(object);
	}
	
	public void setSubFolder(String parentId, IFolder folder) throws Exception {
		if ( !DBPlugin.isVersionned(parentId) )
			parentId = DBPlugin.generateId(parentId, getProjectId(), getVersion());
				
		IFolder parent = searchFolderById(getFolders(), parentId);
		
		if ( parent == null )
			throw new Exception("Cannot find folder (id = " + parentId);
		
		parent.getFolders().add(folder);
	}
	
	public IFolder searchFolderById(List<IFolder> _folders, String _id) {
		for ( IFolder folder: _folders ) {
			if ( _id.equals(folder.getId()) )
				return folder;
			IFolder subFolder = searchFolderById(folder.getFolders(), _id);
			if ( subFolder != null ) return subFolder;
		}
		return null;
	}

	/**
	 * Search a folder by name.
	 *  
	 * @param _name
	 * @return IFolder
	 */
	public IFolder searchProjectFolderByName(String _name) {
		for ( IFolder f: model.getFolders() ) {
			if ( f.getName().equals(DBPlugin.SharedFolderName) ) {
				//we search in the project folder only
				for ( IFolder ff: f.getFolders() ) {
					if ( _name.equals(ff.getName()) ) {
						return ff;
					}
				}
			}
		}
		return null;
	}

	public int countElements() {
		return allElements==null ? nbElements : allElements.size();
	}
	public int countRelationships() {
		return allRelationships==null ? nbRelationships : allRelationships.size();
	}
	public int countArchimateDiagramModels() {
		return allArchimateDiagramModels==null ? nbArchimateDiagramModels : allArchimateDiagramModels.size();
	}
	public int countCanvasModels() {
		return allCanvasModels==null ? nbCanvasModels : allCanvasModels.size();
	}
	public int countCanvasModelBlocks() {
		return allCanvasModelBlocks==null ? nbCanvasModelBlocks : allCanvasModelBlocks.size();
	}
	public int countCanvasModelConnections() {
		return allCanvasModelConnections==null ? nbCanvasModelConnections : allCanvasModelConnections.size();
	}
	public int countCanvasModelImages() {
		return allCanvasModelImages==null ? nbCanvasModelImages : allCanvasModelImages.size();
	}
	public int countCanvasModelStickys() {
		return allCanvasModelStickys==null ? nbCanvasModelStickys : allCanvasModelStickys.size();
	}
	public int countDiagramModelArchimateConnections() {
		return allDiagramModelArchimateConnections==null ? nbDiagramModelArchimateConnections : allDiagramModelArchimateConnections.size();
	}
	public int countDiagramModelArchimateObjects() {
		return allDiagramModelArchimateObjects==null ? nbDiagramModelArchimateObjects : allDiagramModelArchimateObjects.size();
	}
	public int countDiagramModelConnections() {
		return allDiagramModelConnections==null ? nbDiagramModelConnections : allDiagramModelConnections.size();
	}
	public int countDiagramModelReferences() {
		return allDiagramModelReferences==null ? nbDiagramModelReferences : allDiagramModelReferences.size();
	}
	public int countDiagramModelGroups() {
		return allDiagramModelGroups==null ? nbDiagramModelGroups : allDiagramModelGroups.size();
	}
	public int countDiagramModelNotes() {
		return allDiagramModelNotes==null ? nbDiagramModelNotes : allDiagramModelNotes.size();
	}
	public int countFolders() {
		return allFolders==null ? nbFolders : allFolders.size();
	}
	public int countMetadatas() {
		return nbMetadatas;
	}
	public int countProperties() {
		return nbProperties;
	}
	public int countSketchModels() {
		return allSketchModels==null ? nbSketchModels : allSketchModels.size();
	}
	public int countSketchModelActors() {
		return allSketchModelActors==null ? nbSketchModelActors : allSketchModelActors.size();
	}
	public int countSketchModelStickys() {
		return allSketchModelStickys==null ? nbSketchModelStickys : allSketchModelStickys.size();
	}
	public int countDiagramModelBendpoints() {
		return nbDiagramModelBendpoints;
	}
	public int countImages() {
		return allImagePaths==null ? nbImagePaths : allImagePaths.size();
	}
	public Set<String> getImagePaths() {
		return allImagePaths;
	}
	public int countAllComponents() {
		return countMetadatas() + countFolders() + countElements() + countRelationships() + countProperties() +
				countArchimateDiagramModels() + countDiagramModelArchimateObjects() + countDiagramModelArchimateConnections() +  countDiagramModelConnections() +
				countDiagramModelGroups() + countDiagramModelNotes() +  
				countCanvasModels() + countCanvasModelBlocks() + countCanvasModelStickys() + countCanvasModelConnections() + countCanvasModelImages() + 
				countSketchModels() + countSketchModelActors() + countSketchModelStickys() + +
				countDiagramModelBendpoints() + countDiagramModelReferences() + countImages();
	}
	
	public void declareChild(String _parent, EObject _child) {
		if ( _parent != null ) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "declarechild(\""+_parent+"\", "+((IIdentifier)_child).getId()+"["+_child.eClass().getName()+"])");
			ArrayList<EObject> children = family.get(_parent);
			if ( children == null ) {
				children = new ArrayList<EObject>();
				family.put(_parent, children);
			}
			children.add(_child);
		}
	}
	
	public void resolveChildren() throws Exception {
		for ( String parentId: Collections.list(family.keys()) ) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "resolveChildren : \""+parentId+"\"");
			EObject parent = searchEObjectById(DBPlugin.generateId(parentId, getProjectId(), getVersion()));
			if ( parent == null )
				throw new Exception("Cannot set children to parent " + parentId + " as we do not know it !!!");

			EList<IDiagramModelObject> children;

			switch ( parent.eClass().getName() ) {
			case "SketchModel" :				children = ((ISketchModel)parent).getChildren(); break;
			case "ArchimateDiagramModel" :		children = ((IArchimateDiagramModel)parent).getChildren(); break;
			case "SketchModelSticky" :			children = ((ISketchModelSticky)parent).getChildren(); break;
			case "CanvasModel" :				children = ((ICanvasModel)parent).getChildren(); break;
			case "DiagramModelGroup" :			children = ((IDiagramModelGroup)parent).getChildren(); break;
			case "DiagramModelArchimateObject" :children = ((IDiagramModelArchimateObject)parent).getChildren(); break;
			case "CanvasModelBlock" :			children = ((ICanvasModelBlock)parent).getChildren(); break;
			default :
				throw new Exception("Don't know how to resolve children for " + ((INameable)parent).getName() + " (" + parent.eClass().getName() + ")");
			}
			
			if ( children == null )
				throw new Exception("resolveChildren() : should resolve children but do not find them for parent " + ((INameable)parent).getName() + " (" + parent.eClass().getName() + ")");

			for ( EObject child: family.get(parentId) ) {
				if ( child == null )
					throw new Exception("resolveChildren() : should add child but it is null, parent is " + ((INameable)parent).getName() + " (" + parent.eClass().getName() + ")");
				DBPlugin.debug(DebugLevel.Variable, "resolveChildren() : adding child "+((IIdentifier)child).getId()+"["+child.eClass().getName()+"] to parent "+((IIdentifier)parent).getId());
				if ( child.eClass().getName().equals("DiagramModelArchimateObject"))
					children.add((IDiagramModelArchimateObject)child);
				else
					children.add((IDiagramModelObject)child);
			}
		}
	}
	
	public void declareSourceConnection(String _parent, EObject _child) {
		if ( _parent != null ) {
			ArrayList<EObject> children = sourceConnections.get(_parent);
			if ( children == null ) {
				children = new ArrayList<EObject>();
				sourceConnections.put(_parent, children);
			}
			children.add(_child);
		}
	}
	public void resolveSourceConnections() {
		for ( String parentId: Collections.list(sourceConnections.keys()) ) {
			EObject parent = searchEObjectById(DBPlugin.generateId(parentId, getProjectId(), getVersion()));
			if ( parent == null ) {
				System.out.println("Cannot set sourceConnections to parent " + parentId + " as we do not know it !!!");
			} else {
				for ( EObject child: sourceConnections.get(parentId) ) {
					((IDiagramModelObject)parent).getSourceConnections().add((IDiagramModelConnection)child);
				}
			}
		}
	}
}