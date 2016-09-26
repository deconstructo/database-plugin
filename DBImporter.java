/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.archicontribs.database.DBPlugin.DebugLevel;
import org.archicontribs.database.DBPlugin.Level;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.canvas.model.ICanvasFactory;
import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.canvas.model.ICanvasModelBlock;
import com.archimatetool.canvas.model.ICanvasModelConnection;
import com.archimatetool.canvas.model.ICanvasModelImage;
import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.model.IModelImporter;
import com.archimatetool.editor.model.ISelectedModelImporter;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelGroup;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelReference;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.ISketchModelActor;
import com.archimatetool.model.ISketchModelSticky;

/** This will work for Java 8 and onward 
 * 
 */
import java.util.Base64;

//
//v�rifier si des vues ou des relations d'autres mod�les r�f�rencent des objets disparus
//pour �a, utiliser la transaction :
//  1 - cr�er transaction
//  2 - sauvegarder le mod�le
//  3 - demander � l'utilisateur
//				soit on modifie les autres projets pour que les vues et les relations pointent vers la nouvelle version des objets
//				soit on ne les modifie pas
//				soit on utilise une propri�t� pour le sp�cifier, objet par objet
//  4 - si des mod�les sont modifi�s par cette op�ration, alors il faut auto-g�n�rer une nouvelle version
//

/**
 * Import from Database
 * 
 * @author Herv� JOUIN
 */
public class DBImporter implements IModelImporter, ISelectedModelImporter {
	private Connection db;
	
	HashMap<String, HashMap<String, String>> selectedModels;
	DBModel dbModel;
	HashMap<String,String> modelSelected;
	private DBSelectModel dbSelectModel;
	private DBProgress dbProgress;
	private DBProgressTabItem dbTabItem;
	
	private int countMetadatas;
	private int countFolders;
	private int countElements;
	private int countRelationships;
	private int countProperties;
	private int countArchimateDiagramModels;
	private int countDiagramModelArchimateConnections;
	private int countDiagramModelConnections;
	private int countDiagramModelReferences;
	private int countDiagramModelArchimateObjects;
	private int countDiagramModelGroups;
	private int countDiagramModelNotes;
	private int countCanvasModels;
	private int countCanvasModelBlocks;
	private int countCanvasModelStickys;
	private int countCanvasModelConnections;
	private int countCanvasModelImages;
	private int countImages;
	private int countSketchModels;
	private int countSketchModelActors;
	private int countSketchModelStickys;
	private int countDiagramModelBendpoints;
	private int countTotal;
	
	private int totalMetadatas;;
	private int totalFolders;
	private int totalElements;
	private int totalRelationships;
	private int totalProperties;
	private int totalArchimateDiagramModels;
	private int totalDiagramModelArchimateConnections;
	private int totalDiagramModelConnections;
	private int totalDiagramModelArchimateObjects;
	private int totalDiagramModelGroups;
	private int totalDiagramModelNotes;
	private int totalCanvasModels;
	private int totalCanvasModelBlocks;
	private int totalCanvasModelStickys;
	private int totalCanvasModelConnections;
	private int totalCanvasModelImages;
	private int totalImages;
	private int totalSketchModels;
	private int totalSketchModelActors;
	private int totalSketchModelStickys;
	private int totalDiagramModelBendpoints;
	private int totalDiagramModelReferences;
	
	private int totalInDatabase;
	
	@Override
	public void doImport() throws IOException {
		DBPlugin.debug(DebugLevel.MainMethod, "DBImporter.doImport()");
		doImport(null);
	}

	@Override
	public void doImport(IArchimateModel _model) throws IOException {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.doImport("+(_model==null?"null":_model.getName())+")");
		dbSelectModel = new DBSelectModel();
		try {
			db = dbSelectModel.selectModelToImport();
		} catch (Exception err) {
			DBPlugin.popup(Level.Error, "An error occurred !!!", err);
			DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.doImport("+(_model==null?"null":_model.getName())+")");
			return;
		}
		
		if ( db == null ) {	// if there is no database connection, we cannot export
			DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.doImport("+(_model==null?"null":_model.getName())+")");
			return;
		}
		
		selectedModels = dbSelectModel.getSelectedModels();
		if ( selectedModels == null || selectedModels.size() == 0 ) {
			// if the user clicked on cancel or did not selected any project to export, we give up
			try { db.close(); } catch (SQLException ee) { };
			DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.doImport("+(_model==null?"null":_model.getName())+")");
			return;
		}
		
		dbProgress = new DBProgress();
		try {
			dbModel = null;

			//TODO
			// Si on r�f�rence des objets d'autres mod�les (dans une vue ou dans des relations)
			// alors, proposer � l'utilisateur
			//		soit charger les autres projets (en r�cursifs car ils peuvent d�pendre les uns des autres)
			//		soit charger uniquement les objets d�pendants dans un dossier sp�cial (mais attention � la sauvegarde)
			//		soit ne pas les charger mais ils devront �tre reconduits lors de la sauvegarde
			//

			for ( HashMap<String, String> x: selectedModels.values() ) {
				modelSelected=x;
			}
			if ( modelSelected.get("mode").equals("Shared") ) {
				// in shared mode, the database models will be loaded in folders in a generic model
				DBPlugin.debug(DebugLevel.Variable,"Using existing model "+modelSelected.get("name"));
				dbModel = new DBModel();
				if ( dbModel.getProjectsFolders() != null ) {
					// we deny to import a model twice as this will create ID conflicts
					for (IFolder f: dbModel.getProjectsFolders() ) {
						if ( f.getId().split(DBPlugin.Separator)[0].equals(modelSelected.get("id")) ) {
							DBPlugin.popup(Level.Error, "You cannot import model \""+modelSelected.get("name")+"\" twice in shared mode.");
							try { db.close(); } catch (Exception ee) {}
							DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.doImport("+(_model==null?"null":_model.getName())+")");
							return;
						}
					}
				}
				dbModel.addFolder(modelSelected.get("id"), modelSelected.get("version"), modelSelected.get("name"), modelSelected.get("purpose"));
			} else {
				// in standalone mode, we import the database model in a dedicated Archi model
				DBPlugin.debug(DebugLevel.Variable,"Creating new model "+modelSelected.get("name"));
				dbModel = new DBModel(IArchimateFactory.eINSTANCE.createArchimateModel());
				dbModel.getModel().setDefaults();
				dbModel.setProjectId(modelSelected.get("id"), modelSelected.get("version"));
				dbModel.setName(modelSelected.get("name"));
				dbModel.setPurpose(modelSelected.get("purpose"));
				IEditorModelManager.INSTANCE.registerModel(dbModel.getModel());
			}

			long startTime = System.currentTimeMillis();
			
			//we show up the progressbar
			dbTabItem = dbProgress.tabItem(modelSelected.get("name"));
			DBPlugin.debug(DebugLevel.Variable,"Please wait while counting and versionning components ...");
			dbTabItem.setText("Please wait while counting and versionning components ...");

			dbModel.initializeIndex();
			
			countMetadatas = 0;
			countFolders = 0;
			countElements = 0;
			countRelationships = 0;
			countProperties = 0;
			countArchimateDiagramModels = 0;
			countDiagramModelArchimateConnections = 0;
			countDiagramModelConnections = 0;
			countDiagramModelReferences = 0;
			countDiagramModelArchimateObjects = 0;
			countDiagramModelGroups = 0;
			countDiagramModelNotes = 0;
			countCanvasModels = 0;
			countCanvasModelBlocks = 0;
			countCanvasModelStickys = 0;
			countCanvasModelConnections = 0;
			countCanvasModelImages = 0;
			countImages = 0;
			countSketchModels = 0;
			countSketchModelActors = 0;
			countSketchModelStickys = 0;
			countDiagramModelBendpoints = 0;
			countTotal = 0;

			totalMetadatas=Integer.parseInt(modelSelected.get("countMetadatas"));
			totalFolders=Integer.parseInt(modelSelected.get("countFolders"));
			totalElements=Integer.parseInt(modelSelected.get("countElements"));
			totalRelationships=Integer.parseInt(modelSelected.get("countRelationships"));
			totalProperties=Integer.parseInt(modelSelected.get("countProperties"));
			totalArchimateDiagramModels=Integer.parseInt(modelSelected.get("countArchimateDiagramModels"));
			totalDiagramModelArchimateConnections=Integer.parseInt(modelSelected.get("countDiagramModelArchimateConnections"));
			totalDiagramModelConnections=Integer.parseInt(modelSelected.get("countDiagramModelConnections"));
			totalDiagramModelArchimateObjects=Integer.parseInt(modelSelected.get("countDiagramModelArchimateObjects"));
			totalDiagramModelGroups=Integer.parseInt(modelSelected.get("countDiagramModelGroups"));
			totalDiagramModelNotes=Integer.parseInt(modelSelected.get("countDiagramModelNotes"));
			totalCanvasModels=Integer.parseInt(modelSelected.get("countCanvasModels"));
			totalCanvasModelBlocks=Integer.parseInt(modelSelected.get("countCanvasModelBlocks"));
			totalCanvasModelStickys=Integer.parseInt(modelSelected.get("countCanvasModelStickys"));
			totalCanvasModelConnections=Integer.parseInt(modelSelected.get("countCanvasModelConnections"));
			totalCanvasModelImages=Integer.parseInt(modelSelected.get("countCanvasModelImages"));
			totalImages=Integer.parseInt(modelSelected.get("countImages"));
			totalSketchModels=Integer.parseInt(modelSelected.get("countSketchModels"));
			totalSketchModelActors=Integer.parseInt(modelSelected.get("countSketchModelActors"));
			totalSketchModelStickys=Integer.parseInt(modelSelected.get("countSketchModelStickys"));
			totalDiagramModelBendpoints=Integer.parseInt(modelSelected.get("countDiagramModelBendpoints"));
			totalDiagramModelReferences=Integer.parseInt(modelSelected.get("countDiagramModelReferences"));
			
			totalInDatabase = totalMetadatas + totalFolders + totalElements + totalRelationships + totalProperties +
					totalArchimateDiagramModels + totalDiagramModelArchimateObjects + totalDiagramModelArchimateConnections + totalDiagramModelGroups + totalDiagramModelNotes +  
					totalCanvasModels + totalCanvasModelBlocks + totalCanvasModelStickys + totalCanvasModelConnections + totalCanvasModelImages + 
					totalSketchModels + totalSketchModelActors + totalSketchModelStickys + totalDiagramModelConnections +
					totalDiagramModelBendpoints + totalDiagramModelReferences + totalImages;
			
			dbTabItem.setMaximum(totalInDatabase);
			DBPlugin.debug(DebugLevel.Variable,"Please wait while importing components ...");
			dbTabItem.setText("Please wait while importing components ...");

			// we import the model objects
			importModelProperties(dbModel);
			importFolders(dbModel);
			importArchimateElement(dbModel);
			importRelationship(dbModel);

			importArchimateDiagramModel(dbModel);

			importCanvasModel(dbModel);
			importCanvasModelBlock(dbModel);
			importCanvasModelSticky(dbModel);
			importCanvasModelImage(dbModel);

			importSketchModel(dbModel);
			importSketchModelActor(dbModel);
			importSketchModelSticky(dbModel);

			importDiagramModelArchimateObject(dbModel);

			importDiagramModelReference(dbModel);

			importConnections(dbModel);
			importTargetConnections(dbModel);
			
			importImages(dbModel);

			DBPlugin.debug(DebugLevel.Variable,"Please wait while resolving dependencies ...");
			dbTabItem.setText("Please wait while resolving dependencies ...");
			dbModel.resolveSourceConnections();
			dbModel.resolveChildren();

			long endTime = System.currentTimeMillis();
			
			String duration = String.format("%d'%02d", TimeUnit.MILLISECONDS.toMinutes(endTime-startTime), TimeUnit.MILLISECONDS.toSeconds(endTime-startTime)-TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endTime-startTime)));
			
			String msg;
			
			if ( countTotal == totalInDatabase ) {
				msg = "The model \"" + dbModel.getName() + "\" has been successfully imported from the database in "+duration+"\n\n";
				msg += "--> " + countTotal + " components imported <--";
				dbTabItem.setSuccess(msg);
			} else {
				msg = "The model \"" + dbModel.getName() + "\" has been imported from the database in "+duration+", but with errors !\nPlease check below :\n";
				msg += "--> "+ countTotal + "/" + totalInDatabase + " components imported <--";
				dbTabItem.setError(msg);
			}
			DBPlugin.debug(DebugLevel.Variable,msg);
			
		} catch (Exception e) {
			dbTabItem.setError("An error occured while importing the model from the database.\nThe model imported into Archi might be incomplete, please check below : \n" + e.getClass().getSimpleName() + " : " + e.getMessage());
			
			DBPlugin.popup(Level.Error, "An error occured while importing the model from the database.\n\nThe model imported into Archi might be incomplete !!!", e);			
		}
		
		dbTabItem.setCountMetadatas(countMetadatas, totalMetadatas);
		
		dbTabItem.setCountFolders(countFolders, totalFolders);
		dbTabItem.setCountElements(countElements, totalElements);
		dbTabItem.setCountRelationships(countRelationships, totalRelationships);
		dbTabItem.setCountProperties(countProperties, totalProperties);

		dbTabItem.setCountArchimateDiagramModels(countArchimateDiagramModels, totalArchimateDiagramModels);
		dbTabItem.setCountDiagramModelArchimateObjects(countDiagramModelArchimateObjects, totalDiagramModelArchimateObjects);
		dbTabItem.setCountDiagramModelArchimateConnections(countDiagramModelArchimateConnections, totalDiagramModelArchimateConnections);
		dbTabItem.setCountDiagramModelConnections(countDiagramModelConnections, totalDiagramModelConnections);

		dbTabItem.setCountDiagramModelGroups(countDiagramModelGroups, totalDiagramModelGroups);
		dbTabItem.setCountDiagramModelNotes(countDiagramModelNotes, totalDiagramModelNotes);

		dbTabItem.setCountCanvasModels(countCanvasModels, totalCanvasModels);
		dbTabItem.setCountCanvasModelBlocks(countCanvasModelBlocks, totalCanvasModelBlocks);
		dbTabItem.setCountCanvasModelStickys(countCanvasModelStickys, totalCanvasModelStickys);
		dbTabItem.setCountCanvasModelConnections(countCanvasModelConnections, totalCanvasModelConnections);
		dbTabItem.setCountCanvasModelImages(countCanvasModelImages, totalCanvasModelImages);

		dbTabItem.setCountSketchModels(countSketchModels, totalSketchModels);
		dbTabItem.setCountSketchModelActors(countSketchModelActors, totalSketchModelActors);
		dbTabItem.setCountSketchModelStickys(countSketchModelStickys, totalSketchModelStickys);

		dbTabItem.setCountDiagramModelBendpoints(countDiagramModelBendpoints, totalDiagramModelBendpoints);
		dbTabItem.setCountDiagramModelReferences(countDiagramModelReferences, totalDiagramModelReferences);

		dbTabItem.setCountImages(countImages, totalImages);
		
		dbTabItem.finish();
		dbProgress.finish();
		try { db.close(); } catch (Exception ee) {}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.doImport("+(_model==null?"null":_model.getName())+")");
	}


	private void importArchimateElement(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importArchimateElement()");
		
		ResultSet result;
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT id, documentation, folder, name, type FROM archimateelement WHERE model = ? AND version = ?",
					_dbModel.getProjectId(),
					_dbModel.getVersion()
					);
		} else {
			result = DBPlugin.select(db, "MATCH (e:archimateelement)-[:isInModel]->(m:model {model:?, version:?}) RETURN e.id as id, e.documentation as documentation, e.folder as folder, e.name as name, e.type as type",
					_dbModel.getProjectId(),
					_dbModel.getVersion());
		}
		
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing "+result.getString("type")+" id="+result.getString("id")+" name="+result.getString("name"));
			IArchimateElement archimateElement = (IArchimateElement)IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type")));
			dbTabItem.setCountElements(++countElements);
			dbTabItem.setProgressBar(++countTotal);
			
			archimateElement.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion())); 
			archimateElement.setName(result.getString("name"));
			archimateElement.setDocumentation(result.getString("documentation"));
			
			_dbModel.setFolder(result.getString("folder"),archimateElement);

			_dbModel.indexEObject(archimateElement);

			importObjectProperties(_dbModel, archimateElement);
		}
		
		dbTabItem.setCountElements(countElements);
		dbTabItem.setProgressBar(countTotal);
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importArchimateElement()");
	}

	private void importRelationship(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importRelationship()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT id, documentation, name, source, target, type, folder FROM relationship WHERE model = ? AND version = ?",
					_dbModel.getProjectId(), _dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (m:model {model:?, version:?}), (s)-[:isInModel]->(m), (t)-[:isInModel]->(m), (s)-[r]->(t) RETURN r.type as type, r.id as id, r.documentation as documentation, r.name as name, s.id as source, t.id as target, r.type, r.folder as folder", 
					_dbModel.getProjectId(), _dbModel.getVersion(),
					_dbModel.getProjectId(), _dbModel.getVersion());
		}
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing "+result.getString("type")+" id="+result.getString("id")+" source="+result.getString("source")+" target="+result.getString("target"));
			IArchimateRelationship relationship = (IArchimateRelationship)IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type")));
			dbTabItem.setCountRelationships(++countRelationships);
			dbTabItem.setProgressBar(++countTotal);
			
			relationship.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion()));
			relationship.setName(result.getString("name"));
			relationship.setDocumentation(result.getString("documentation"));
			relationship.setSource((IArchimateElement)dbModel.searchEObjectById(DBPlugin.generateId(result.getString("source"), _dbModel.getProjectId(), _dbModel.getVersion())));
			relationship.setTarget((IArchimateElement)dbModel.searchEObjectById(DBPlugin.generateId(result.getString("target"), _dbModel.getProjectId(), _dbModel.getVersion())));
			
			_dbModel.setFolder(result.getString("folder"), relationship);

			_dbModel.indexEObject(relationship);

			importObjectProperties(_dbModel, relationship);
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importRelationship()");
	}

	private void importArchimateDiagramModel(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importArchimateDiagramModel()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT id, connectionroutertype, documentation, folder, name, type, viewpoint FROM archimatediagrammodel WHERE model = ? AND version = ?",
					_dbModel.getProjectId(),
					_dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (e:archimatediagrammodel)-[:isInModel]->(m:model {model:?, version:?}) RETURN e.id as id, e.connectionroutertype as connectionroutertype, e.documentation as documentation, e.folder as folder, e.name as name, e.type as type, e.viewpoint as viewpoint", 
					_dbModel.getProjectId(),
					_dbModel.getVersion());
		}
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing "+result.getString("type")+" id="+result.getString("id")+" name="+result.getString("name"));
			IArchimateDiagramModel archimatediagramModel = (IArchimateDiagramModel)IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type")));
			dbTabItem.setCountArchimateDiagramModels(++countArchimateDiagramModels);
			dbTabItem.setProgressBar(++countTotal);
			
			archimatediagramModel.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion()));
			archimatediagramModel.setName(result.getString("name"));
			archimatediagramModel.setDocumentation(result.getString("documentation"));
			archimatediagramModel.setConnectionRouterType(result.getInt("connectionroutertype"));
			archimatediagramModel.setViewpoint(result.getString("viewpoint"));
			
			_dbModel.setFolder(result.getString("folder"), archimatediagramModel);

			_dbModel.indexEObject(archimatediagramModel);

			importObjectProperties(_dbModel, archimatediagramModel);
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importArchimateDiagramModel()");
	}
	private void importDiagramModelArchimateObject(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importDiagramModelArchimateObject()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT * FROM diagrammodelarchimateobject WHERE model = ? AND version = ? ORDER BY indent, rank, parent",
					_dbModel.getProjectId(), _dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (e:diagrammodelarchimateobject)-[:isInModel]->(m:model {model:?, version:?}) RETURN e.id as id, e.fillcolor as fillcolor, e.font as font, e.fontcolor as fontcolor, e.linecolor as linecolor, e.linewidth as linewidth, e.textalignment as textalignment, e.x as x, e.y as y, e.width as width, e.height as height, e.class as class, e.archimateelementid as archimateelementid, e.archimateelementname as archimateelementname, e.archimateelementclass as archimateelementclass, e.name as name, e.documentation as documentation, e.bordertype as bordertype, e.content as content, e.type as type, e.parent as parent ORDER BY e.indent, e.rank, e.parent",
					_dbModel.getProjectId(), _dbModel.getVersion());
		}
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing "+result.getString("class")+" id="+result.getString("id")+" name="+result.getString("name"));
			IDiagramModelObject diagramModelObject = (IDiagramModelObject)IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("class")));
			//setting common properties
			diagramModelObject.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion()));
			diagramModelObject.setFillColor(result.getString("fillcolor"));
			diagramModelObject.setFont(result.getString("font"));
			diagramModelObject.setFontColor(result.getString("fontcolor"));
			diagramModelObject.setLineColor(result.getString("linecolor"));
			diagramModelObject.setLineWidth(result.getInt("linewidth"));
			diagramModelObject.setTextAlignment(result.getInt("textalignment"));
			diagramModelObject.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			//setting specific properties
			switch (result.getString("class")) {
			case "DiagramModelArchimateObject" :
				IDiagramModelArchimateObject diagramModelArchimateObject = (IDiagramModelArchimateObject)diagramModelObject;

				if ( dbModel.getProjectId().equals(DBPlugin.getProjectId(result.getString("archimateelementid"))) ) {
					// if we are in the same model, then the corresponding archimate element should already exist
					// if we are not in the same model, the we should wait that wll the projets are imported to resolve it
					// //TODO: BY THE WAY, ALWAYS WAIT THE END TO RESOLVE IT, IT WILL BE SIMPLIER
					IArchimateElement child = (IArchimateElement)dbModel.searchEObjectById(result.getString("archimateelementid"));
					if ( child == null )
						throw new Exception("importDiagramModelArchimateObject : Unknown ArchimateElement " + result.getString("archimateelementid"));
					diagramModelArchimateObject.setArchimateElement(child);
										//} else {
										//	IArchimateElement child = (IArchimateElement)IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("archimateelementclass")));
										//		((EObjectImpl)child).eSetProxyURI(URI.createURI(result.getString("archimateelementid")));
										//		child.setId(result.getString("archimateelementid"));
										//		child.setName(DBPlugin.generateId(DBPlugin.getProjectId(result.getString("archimateelementid")),result.getString("archimateelementname"),null);
										//		child.setDocumentation("Load model ID '"+DBPlugin.getProjectId(result.getString("archimateelementid"))+"' to import element.");
										//		DBPlugin.popup(Level.Error,"   setting proxy to ArchimateElement "  + child.getName() + " ("+child.getId()+")");
										//		//TODO: replace by throw exception
										//		((IDiagramModelArchimateObject)iDiagramModelObject).setArchimateElement(child);
										//	}
				}
				diagramModelArchimateObject.setType(result.getInt("type"));
				
				_dbModel.indexEObject(diagramModelArchimateObject);
				_dbModel.declareChild(result.getString("parent"), diagramModelArchimateObject);
				
				dbTabItem.setCountDiagramModelArchimateObjects(++countDiagramModelArchimateObjects);
				dbTabItem.setProgressBar(++countTotal);
				break;
			case "DiagramModelGroup" :
				IDiagramModelGroup diagramModelGroup = (IDiagramModelGroup)diagramModelObject;
				diagramModelGroup.setName(result.getString("name"));
				diagramModelGroup.setDocumentation(result.getString("documentation"));
				
				importObjectProperties(dbModel, diagramModelGroup);
				
				_dbModel.indexEObject(diagramModelGroup);
				_dbModel.declareChild(result.getString("parent"), diagramModelGroup);
				
				dbTabItem.setCountDiagramModelGroups(++countDiagramModelGroups);
				dbTabItem.setProgressBar(++countTotal);
				break;
			case "DiagramModelNote" :
				IDiagramModelNote diagramModelNote = (IDiagramModelNote)diagramModelObject;
				diagramModelNote.setName(result.getString("name"));
				diagramModelNote.setBorderType(result.getInt("bordertype"));
				diagramModelNote.setContent(result.getString("content"));
				
				_dbModel.indexEObject(diagramModelNote);
				_dbModel.declareChild(result.getString("parent"), diagramModelNote);
				
				dbTabItem.setCountDiagramModelNotes(++countDiagramModelNotes);
				dbTabItem.setProgressBar(++countTotal);
				break;
			default :
				throw new Exception("importDiagramModelArchimateObject() : Don't know how to import " + result.getString("class"));
			}
		}
		result.close();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importDiagramModelArchimateObject()");
	}

	@SuppressWarnings("deprecation")
	private void importConnections(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importConnections()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT * FROM connection WHERE model = ? AND version = ? ORDER BY parent, rank",
					_dbModel.getProjectId(),
					_dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (e:connection)-[:isInModel]->(m:model {model:?, version:?}) RETURN e.id as id, e.relationship as relationship, e.class as class, e.documentation as documentation, e.font as font, e.fontcolor as fontcolor, e.linecolor as linecolor, e.linewidth as linewidth, e.source as source, e.target as target, e.text as text, e.textposition as textposition, e.type as type, e.parent as parent ORDER BY e.parent, e.rank",
					_dbModel.getProjectId(),
					_dbModel.getVersion());
		}
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing "+result.getString("class")+" id="+result.getString("id")+" source="+result.getString("source")+" target="+result.getString("target"));
			IDiagramModelConnection diagramModelConnection;
			if ( result.getString("class").equals("CanvasModelConnection") )
				diagramModelConnection = ICanvasFactory.eINSTANCE.createCanvasModelConnection();
			else
				diagramModelConnection = (IDiagramModelConnection)IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("class")));
			diagramModelConnection.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(), _dbModel.getVersion()));
			diagramModelConnection.setDocumentation(result.getString("documentation"));
			diagramModelConnection.setFont(result.getString("font"));
			diagramModelConnection.setFontColor(result.getString("fontcolor"));
			diagramModelConnection.setLineColor(result.getString("linecolor"));
			diagramModelConnection.setLineWidth(result.getInt("linewidth"));
			diagramModelConnection.setSource((IDiagramModelObject)_dbModel.searchEObjectById(DBPlugin.generateId(result.getString("source"), _dbModel.getProjectId(), _dbModel.getVersion())));
			diagramModelConnection.setTarget((IDiagramModelObject) _dbModel.searchEObjectById(DBPlugin.generateId(result.getString("target"), _dbModel.getProjectId(), _dbModel.getVersion())));
			diagramModelConnection.setText(result.getString("text"));
			diagramModelConnection.setTextPosition(result.getInt("textposition"));
			diagramModelConnection.setType(result.getInt("type"));
			
			_dbModel.declareSourceConnection(result.getString("parent"), diagramModelConnection);
			
			importObjectProperties(dbModel, diagramModelConnection);
			
			// Bendpoints
			ResultSet resultBendpoints;
			if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
				resultBendpoints = DBPlugin.select(db, "SELECT startx, starty, endx, endy FROM bendpoint WHERE parent = ? AND model = ? AND version = ? ORDER BY rank",
						result.getString("id"),
						_dbModel.getProjectId(),
						_dbModel.getVersion()
						);
			} else {
				resultBendpoints = DBPlugin.select(db, "MATCH (e:bendpoint {parent:?})-[:isInModel]->(m:model {model:?, version:?}) RETURN e.startx as startx, e.starty as starty, e.endx as endx, e.endy as endy",
						result.getString("id"),
						_dbModel.getProjectId(),
						_dbModel.getVersion()
						);
			}
			
			while(resultBendpoints.next()) {
				IDiagramModelBendpoint bendpoint = IArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
				++countDiagramModelBendpoints;
				++countTotal;
				
				bendpoint.setStartX(resultBendpoints.getInt("startx"));
				bendpoint.setStartY(resultBendpoints.getInt("starty"));
				bendpoint.setEndX(resultBendpoints.getInt("endx"));
				bendpoint.setEndY(resultBendpoints.getInt("endy"));
				diagramModelConnection.getBendpoints().add(bendpoint);
				_dbModel.indexEObject(bendpoint);
			}
			resultBendpoints.close();
			
			dbTabItem.setCountDiagramModelBendpoints(countDiagramModelBendpoints);
			dbTabItem.setProgressBar(countTotal);
			
			switch (result.getString("class")) {
			case "DiagramModelConnection" :
				_dbModel.indexEObject(diagramModelConnection);
				
				dbTabItem.setCountDiagramModelConnections(++countDiagramModelConnections);
				dbTabItem.setProgressBar(++countTotal);
				break;
			case "DiagramModelArchimateConnection" :
				IDiagramModelArchimateConnection diagramModelArchimateConnection = (IDiagramModelArchimateConnection)diagramModelConnection;

				IArchimateRelationship relation = (IArchimateRelationship)dbModel.searchEObjectById(DBPlugin.generateId(result.getString("relationship"), _dbModel.getProjectId(),_dbModel.getVersion()));
				if ( relation == null )
					throw new Exception("importConnections() : cannot find relationship "+DBPlugin.generateId(result.getString("relationship"), _dbModel.getProjectId(),_dbModel.getVersion()));
				diagramModelArchimateConnection.setArchimateRelationship(relation);
				
				_dbModel.indexEObject(diagramModelArchimateConnection);
				
				dbTabItem.setCountDiagramModelArchimateConnections(++countDiagramModelArchimateConnections);
				dbTabItem.setProgressBar(++countTotal);
				break;
			case "CanvasModelConnection" :
				ICanvasModelConnection canvasModelConnection = (ICanvasModelConnection)diagramModelConnection;
				
				canvasModelConnection.setLocked(result.getBoolean("islocked"));
				
				_dbModel.indexEObject(canvasModelConnection);
				
				dbTabItem.setCountCanvasModelConnections(++countCanvasModelConnections);
				dbTabItem.setProgressBar(++countTotal);
				break;
			default :									
				throw new Exception("importConnection() : do not know how to import " + result.getString("class"));
			}
		}
		result.close();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importConnections()");
	}

	private void importTargetConnections(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importTargetConnections()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT id, targetconnections FROM diagrammodelarchimateobject WHERE model = ? AND version = ? AND targetconnections IS NOT NULL",
					_dbModel.getProjectId(),
					_dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (e:diagrammodelarchimateobject)-[:isInModel]->(m:model {model:?, version:?}) WHERE e.targetconnections IS NOT NULL RETURN e.id as id, e.targetconnections as targetconnections",
					_dbModel.getProjectId(),
					_dbModel.getVersion());
		}
		
		while(result.next()) {
			IDiagramModelObject diagramModelObject = (IDiagramModelObject)_dbModel.searchEObjectById(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(), _dbModel.getVersion()));
			
			if ( diagramModelObject == null )
				throw new Exception("importTargetConnections() : do not know diagramModelObject " + DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(), _dbModel.getVersion()));
			
			if ( result.getString("targetconnections").length() > 0 ) {
				for ( String target: result.getString("targetconnections").split(",")) {
					IDiagramModelConnection connection = (IDiagramModelConnection)_dbModel.searchEObjectById(DBPlugin.generateId(target, _dbModel.getProjectId(), _dbModel.getVersion()));
					if ( connection == null )
						throw new Exception("Cannot find targetConnection "+DBPlugin.generateId(target, _dbModel.getProjectId(), _dbModel.getVersion()));
					
					diagramModelObject.getTargetConnections().add(connection);
					//_dbModel.indexEObject(diagramModelObject);
				}
			}
		}
		result.close();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importTargetConnections()");
	}

	private void importCanvasModel(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importCanvasModel()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT * FROM canvasmodel WHERE model = ? AND version = ?",
					_dbModel.getProjectId(),
					_dbModel.getVersion()
					);
		} else {
			result = DBPlugin.select(db, "MATCH (e:canvasmodel)-[:isInModel]->(m:model {model:?, version:?}) RETURN e.id as id, e.name as name, e.documentation as documentation, e.hintcontent as hintcontent, e.hinttitle as hinttitle, e.connectionroutertype as connectionroutertype, e.folder as folder",
					_dbModel.getProjectId(),
					_dbModel.getVersion()
					);
		}
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing CanvasModel id="+result.getString("id")+" name="+result.getString("name"));
			ICanvasModel canvasModel = ICanvasFactory.eINSTANCE.createCanvasModel();
			dbTabItem.setCountCanvasModels(++countCanvasModels);
			dbTabItem.setProgressBar(++countTotal);
			
			canvasModel.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion())); 
			canvasModel.setName(result.getString("name"));
			canvasModel.setDocumentation(result.getString("documentation"));
			canvasModel.setHintContent(result.getString("hintcontent"));
			canvasModel.setHintTitle(result.getString("hinttitle"));
			canvasModel.setConnectionRouterType(result.getInt("connectionroutertype"));
			_dbModel.setFolder(result.getString("folder"), canvasModel);

			_dbModel.indexEObject(canvasModel);

			importObjectProperties(_dbModel, canvasModel);
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importCanvasModel()");
	}

	private void importCanvasModelBlock(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importCanvasModelBlock()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT * FROM canvasmodelblock WHERE model = ? AND version = ? ORDER BY parent, rank, indent",
					_dbModel.getProjectId(), _dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (e:canvasmodelblock)-[:isInModel]->(m:model {model:?, version:?}) RETURN e.id as id, e.bordercolor as bordercolor, e.content as content, e.fillcolor as fillcolor, e.font as font, e.fontcolor as fontcolor, e.hintcontent as hintcontent, e.hinttitle as hinttitle, e.linecolor as linecolor, e.linewidth as linewidth, e.name as name, e.textalignment as textalignment, e.textposition as textposition, e.islocked as islocked, e.imageposition as imageposition, e.imagepath as imagepath, e.x as x, e.y as y, e.width as width, e.height as height, e.parent as parent ORDER BY e.parent, e.rank, e.indent",
					_dbModel.getProjectId(), _dbModel.getVersion());
		}
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing CanvasModelBlock id="+result.getString("id")+" name="+result.getString("name"));
			ICanvasModelBlock canvasModelBlock = ICanvasFactory.eINSTANCE.createCanvasModelBlock();
			dbTabItem.setCountCanvasModelBlocks(++countCanvasModelBlocks);
			dbTabItem.setProgressBar(++countTotal);
			
			canvasModelBlock.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion()));
			canvasModelBlock.setBorderColor(result.getString("bordercolor"));
			canvasModelBlock.setContent(result.getString("content"));
			canvasModelBlock.setFillColor(result.getString("fillcolor"));
			canvasModelBlock.setFont(result.getString("font"));
			canvasModelBlock.setFontColor(result.getString("fontcolor"));
			canvasModelBlock.setHintContent(result.getString("hintcontent"));
			canvasModelBlock.setHintTitle(result.getString("hinttitle"));
			canvasModelBlock.setLineColor(result.getString("linecolor"));
			canvasModelBlock.setLineWidth(result.getInt("linewidth"));
			canvasModelBlock.setName(result.getString("name"));
			canvasModelBlock.setTextAlignment(result.getInt("textalignment"));
			canvasModelBlock.setTextPosition(result.getInt("textposition"));
			canvasModelBlock.setLocked(result.getBoolean("islocked"));
			
			canvasModelBlock.setImagePosition(result.getInt("imageposition"));
			canvasModelBlock.setImagePath(result.getString("imagepath"));
			
			_dbModel.indexImagePath(result.getString("imagepath"));
			
			canvasModelBlock.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			_dbModel.indexEObject(canvasModelBlock);

			importObjectProperties(dbModel, canvasModelBlock);

			_dbModel.declareChild(result.getString("parent"), canvasModelBlock);
		}
		result.close();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importCanvasModelBlock()");
	}

	private void importCanvasModelSticky(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importCanvasModelSticky()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT * FROM canvasmodelsticky WHERE model = ? AND version = ? ORDER BY parent, rank, indent",
					_dbModel.getProjectId(), _dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (e:canvasmodelsticky)-[:isInModel]->(m:model {model:?, version:?}) RETURN e.id as id, e.bordercolor as bordercolor, e.content as content, e.fillcolor as fillcolor, e.font as font, e.fontcolor as fontcolor, e.linecolor as linecolor, e.linewidth as linewidth, e.name as name, e.notes as notes, e.textalignment as textalignment, e.textposition as textposition, e.imageposition as imageposition, e.imagepath as imagepath, e.x as x, e.y as y, e.width as width, e.height as height, e.parent as parent ORDER BY e.parent, e.rank, e.indent",
					_dbModel.getProjectId(), _dbModel.getVersion());
		}
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing CanvasModelSticky id="+result.getString("id")+" name="+result.getString("name"));
			ICanvasModelSticky canvasModelSticky = ICanvasFactory.eINSTANCE.createCanvasModelSticky();
			dbTabItem.setCountCanvasModelStickys(++countCanvasModelStickys);
			dbTabItem.setProgressBar(++countTotal);
			
			canvasModelSticky.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion()));
			canvasModelSticky.setBorderColor(result.getString("bordercolor"));
			canvasModelSticky.setContent(result.getString("content"));
			canvasModelSticky.setFillColor(result.getString("fillcolor"));
			canvasModelSticky.setFont(result.getString("font"));
			canvasModelSticky.setFontColor(result.getString("fontcolor"));
			canvasModelSticky.setLineColor(result.getString("linecolor"));
			canvasModelSticky.setLineWidth(result.getInt("linewidth"));
			canvasModelSticky.setName(result.getString("name"));
			canvasModelSticky.setNotes(result.getString("notes"));
			canvasModelSticky.setTextAlignment(result.getInt("textalignment"));
			canvasModelSticky.setTextPosition(result.getInt("textposition"));
			
			canvasModelSticky.setImagePosition(result.getInt("imageposition"));
			canvasModelSticky.setImagePath(result.getString("imagepath"));
			
			_dbModel.indexImagePath(result.getString("imagepath"));

			canvasModelSticky.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			_dbModel.indexEObject(canvasModelSticky);

			importObjectProperties(dbModel, canvasModelSticky);

			_dbModel.declareChild(result.getString("parent"), canvasModelSticky);
		}
		result.close();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importCanvasModelSticky()");
	}

	private void importCanvasModelImage(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importCanvasModelImage()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT * FROM canvasmodelimage WHERE model = ? AND version = ? ORDER BY parent, rank, indent",
					_dbModel.getProjectId(), _dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (e:canvasmodelimage)-[:isInModel]->(m:model {model:?, version:?}) RETURN e.id as id, e.bordercolor as bordercolor, e.islocked as islocked, e.fillcolor as fillcolor, e.font as font, e.fontcolor as fontcolor, e.linecolor as linecolor, e.linewidth as linewidth, e.name as name, e.textalignment as textalignment, e.imagepath as imagepath, e.x as x, e.y as y, e.width as width, e.height as height, e.parent as parent ORDER BY e.parent, e.rank, e.indent",
					_dbModel.getProjectId(), _dbModel.getVersion());
		}
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing CanvasModelImage id="+result.getString("id")+" name="+result.getString("name"));
			ICanvasModelImage canvasModelImage = ICanvasFactory.eINSTANCE.createCanvasModelImage();
			dbTabItem.setCountCanvasModelImages(++countCanvasModelImages);
			dbTabItem.setProgressBar(++countTotal);
			
			canvasModelImage.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion()));
			canvasModelImage.setBorderColor(result.getString("bordercolor"));
			canvasModelImage.setLocked(result.getBoolean("islocked"));
			canvasModelImage.setFillColor(result.getString("fillcolor"));
			canvasModelImage.setFont(result.getString("font"));
			canvasModelImage.setFontColor(result.getString("fontcolor"));
			canvasModelImage.setLineColor(result.getString("linecolor"));
			canvasModelImage.setLineWidth(result.getInt("linewidth"));
			canvasModelImage.setName(result.getString("name"));
			canvasModelImage.setTextAlignment(result.getInt("textalignment"));
			
			canvasModelImage.setImagePath(result.getString("imagepath"));
			
			_dbModel.indexImagePath(result.getString("imagepath"));
			
			canvasModelImage.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			_dbModel.indexEObject(canvasModelImage);

			importObjectProperties(dbModel, canvasModelImage);

			_dbModel.declareChild(result.getString("parent"), canvasModelImage);
		}
		result.close();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importCanvasModelImage()");
	}

	private void importDiagramModelReference(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importDiagramModelReference()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT * FROM DiagramModelReference WHERE model = ? AND version = ? ORDER BY parent, rank, indent",
					_dbModel.getProjectId(), _dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (e:DiagramModelReference)-[:isInModel]->(m:model {model:?, version:?}) RETURN e.id as id, e.fillcolor as fillcolor, e.font as font, e.fontcolor as fontcolor, e.linecolor as linecolor, e.linewidth as linewidth, e.textalignment as textalignment, e.x as x, e.y as y, e.width as width, e.height as height, e.parent as parent ORDER BY e.parent, e.rank, e.indent",
					_dbModel.getProjectId(), _dbModel.getVersion());
		}
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing DiagramModelReference id="+result.getString("id"));
			IDiagramModelReference diagramModelReference = IArchimateFactory.eINSTANCE.createDiagramModelReference();
			dbTabItem.setCountDiagramModelReferences(++countDiagramModelReferences);
			dbTabItem.setProgressBar(++countTotal);
			
			diagramModelReference.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion()));
			diagramModelReference.setFillColor(result.getString("fillcolor"));
			diagramModelReference.setFont(result.getString("font"));
			diagramModelReference.setFontColor(result.getString("fontcolor"));
			diagramModelReference.setLineColor(result.getString("linecolor"));
			diagramModelReference.setLineWidth(result.getInt("linewidth"));
			diagramModelReference.setTextAlignment(result.getInt("textalignment"));
			diagramModelReference.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			_dbModel.indexEObject(diagramModelReference);

			_dbModel.declareChild(result.getString("parent"), diagramModelReference);
		}
		result.close();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importDiagramModelReference()");
	}


	private void importSketchModel(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importSketchModel()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT * FROM sketchmodel WHERE model = ? AND version = ?",
					_dbModel.getProjectId(), _dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (e:sketchmodel)-[:isInModel]->(m:model {model:?, version:?}) RETURN e.id as id, e.name as name, e.documentation as documentation, e.connectionroutertype as connectionroutertype, e.background as background, e.folder as folder",
					_dbModel.getProjectId(), _dbModel.getVersion());
		}
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing SketchModel id="+result.getString("id")+" name="+result.getString("name"));
			ISketchModel sketchModel = IArchimateFactory.eINSTANCE.createSketchModel();
			dbTabItem.setCountSketchModels(++countSketchModels);
			dbTabItem.setProgressBar(++countTotal);
			
			sketchModel.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion())); 
			sketchModel.setName(result.getString("name"));
			sketchModel.setDocumentation(result.getString("documentation"));
			sketchModel.setConnectionRouterType(result.getInt("connectionroutertype"));
			sketchModel.setBackground(result.getInt("background"));

			_dbModel.setFolder(result.getString("folder"), sketchModel);

			importObjectProperties(dbModel, sketchModel);

			_dbModel.indexEObject(sketchModel);
		}
		result.close();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importSketchModel()");
	}

	private void importSketchModelActor(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importSketchModelActor()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT * FROM sketchmodelactor WHERE model = ? AND version = ?",
					_dbModel.getProjectId(), _dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (e:sketchmodelactor)-[:isInModel]->(m:model {model:?, version:?}) RETURN e.id as id, e.fillcolor as fillcolor, e.font as font, e.fontcolor as fontcolor, e.linecolor as linecolor, e.linewidth as linewidth, e.name as name, e.textalignment as textalignment, e.x as x, e.y as y, e.width as width, e.height as height, e.parent as parent",
					_dbModel.getProjectId(), _dbModel.getVersion());
		}
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing SketchModelActor id="+result.getString("id")+" name="+result.getString("name"));
			ISketchModelActor sketchModelActor = IArchimateFactory.eINSTANCE.createSketchModelActor();
			dbTabItem.setCountSketchModelActors(++countSketchModelActors);
			dbTabItem.setProgressBar(++countTotal);
			
			sketchModelActor.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion())); 
			sketchModelActor.setFillColor(result.getString("fillcolor"));
			sketchModelActor.setFont(result.getString("font"));
			sketchModelActor.setFontColor(result.getString("fontcolor"));
			sketchModelActor.setLineColor(result.getString("linecolor"));
			sketchModelActor.setLineWidth(result.getInt("linewidth"));
			sketchModelActor.setName(result.getString("name"));
			sketchModelActor.setTextAlignment(result.getInt("textalignment"));
			sketchModelActor.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			importObjectProperties(dbModel, sketchModelActor);

			_dbModel.indexEObject(sketchModelActor);

			_dbModel.declareChild(result.getString("parent"), sketchModelActor);
		}
		result.close();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importSketchModelActor()");
	}

	private void importSketchModelSticky(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importSketchModelSticky()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT * FROM sketchmodelsticky WHERE model = ? AND version = ?",
					_dbModel.getProjectId(), _dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (e:sketchmodelsticky)-[:isInModel]->(m:model {model:?, version:?}) RETURN e.id as id, e.content as content, e.fillcolor as fillcolor, e.font as font, e.fontcolor as fontcolor, e.linecolor as linecolor, e.linewidth as linewidth, e.name as name, e.textalignment as textalignment, e.x as x, e.y as y, e.width as width, e.height as height, e.parent as parent",
					_dbModel.getProjectId(), _dbModel.getVersion());
		}
		
		while(result.next()) {
			DBPlugin.debug(DebugLevel.Variable, "Importing SketchModelSticky id="+result.getString("id")+" name="+result.getString("name"));
			ISketchModelSticky sketchModelSticky = IArchimateFactory.eINSTANCE.createSketchModelSticky();
			dbTabItem.setCountSketchModelStickys(++countSketchModelStickys);
			dbTabItem.setProgressBar(++countTotal);
			
			sketchModelSticky.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion())); 
			sketchModelSticky.setContent(result.getString("content"));
			sketchModelSticky.setFillColor(result.getString("fillcolor"));
			sketchModelSticky.setFont(result.getString("font"));
			sketchModelSticky.setFontColor(result.getString("fontcolor"));
			sketchModelSticky.setLineColor(result.getString("linecolor"));
			sketchModelSticky.setLineWidth(result.getInt("linewidth"));
			sketchModelSticky.setName(result.getString("name"));
			sketchModelSticky.setTextAlignment(result.getInt("textalignment"));
			sketchModelSticky.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			_dbModel.indexEObject(sketchModelSticky);

			importObjectProperties(dbModel, sketchModelSticky);

			_dbModel.declareChild(result.getString("parent"), sketchModelSticky);
		}
		result.close();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importSketchModelSticky()");
	}


	private void importObjectProperties(DBModel dbModel, EObject parent) throws Exception {
		//DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBImporter.importObjectProperties()");
		
		assert(parent instanceof IProperties);
		assert(parent instanceof IIdentifier);
		
		ResultSet result;
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT name, value FROM property WHERE parent = ? AND model = ? AND version = ? ORDER BY id",
					DBPlugin.getId(((IIdentifier)parent).getId()),
					DBPlugin.getProjectId(((IIdentifier)parent).getId()),
					DBPlugin.getVersion(((IIdentifier)parent).getId())
					);
		} else {
			result = DBPlugin.select(db, "MATCH (e {id:?})-[:isInModel]->(m:model {model:?, version:?}), (e)-[:hasProperty]->(p:property) RETURN p.name as name, p.value as value ORDER BY p.id",
					DBPlugin.getId(((IIdentifier)parent).getId()),
					DBPlugin.getProjectId(((IIdentifier)parent).getId()),
					DBPlugin.getVersion(((IIdentifier)parent).getId())
					);
		}
		 
		while(result.next()) {
			IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
			prop.setKey(result.getString("name"));
			prop.setValue(result.getString("value"));
			((IProperties)parent).getProperties().add(prop);
			dbModel.indexEObject(prop);
			++countProperties;
			++countTotal;
		}
		result.close();
		
		dbTabItem.setCountProperties(countProperties);
		dbTabItem.setProgressBar(countTotal);
		
		//DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBImporter.importObjectProperties()");
	}
	
	private void importModelProperties(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBImporter.importModelProperties()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT id, name, value FROM property WHERE parent = ? AND model = ? AND version = ? ORDER BY id",
					_dbModel.getProjectId(),
					_dbModel.getProjectId(),
					_dbModel.getVersion());
		} else {
			result = DBPlugin.select(db, "MATCH (m:model {model:?, version:?})-[:hasProperty]->(p:property) RETURN p.id as id, p.name as name, p.value as value ORDER BY p.id",
					_dbModel.getProjectId(),
					_dbModel.getVersion());
		}
		
		while(result.next()) {
			IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
			prop.setKey(result.getString("name"));
			prop.setValue(result.getString("value"));
			if ( result.getInt("id") < 0 ) {
				_dbModel.getMetadata().add(prop);
				++countMetadatas;
				_dbModel.indexMetadata();
			} else {
				_dbModel.getProperties().add(prop);
				++countProperties;
			}
			_dbModel.indexEObject(prop);
			++countTotal;
			
		}
		result.close();
		
		dbTabItem.setCountMetadatas(countMetadatas);
		dbTabItem.setCountProperties(countProperties);
		dbTabItem.setProgressBar(countTotal);
		
		DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBImporter.importModelProperties()");
	}
	
	private void importFolders(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importFolders()");
		
		ResultSet result;
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			result = DBPlugin.select(db, "SELECT id, documentation, parent, type, name FROM folder WHERE model = ? AND version = ? ORDER BY rank",
					_dbModel.getProjectId(),
					_dbModel.getVersion()
					);
		} else {
			result = DBPlugin.select(db, "MATCH (f:folder)-[:isInModel]->(m:model {model:?, version:?}) RETURN f.id as id, f.documentation as documentation, f.parent as parent, f.type as type, f.name as name ORDER BY f.rank",
					_dbModel.getProjectId(),
					_dbModel.getVersion()
					);
		}
		
		IFolder folder;
		while(result.next()) {
			if ( result.getInt("type") != 0 ) {				// top level folders have been created at the same time than the model 
				DBPlugin.debug(DebugLevel.Variable, "Using default first level folder \""+result.getString("name")+"\" (type="+result.getInt("type")+", id="+result.getString("id")+", parent="+(result.getString("parent")==null?"null":"\""+result.getString("parent")+"\"")+")");
				folder = _dbModel.getDefaultFolderForFolderType(FolderType.get(result.getInt("type")));
				if ( folder == null )
					throw new Exception("I do not find default folder for type "+result.getInt("type")+" ("+FolderType.get(result.getInt("type")).name()+")");
			} else {
				folder = IArchimateFactory.eINSTANCE.createFolder();
				folder.setName(result.getString("name"));
				
				if ( result.getString("parent") == null ) { // top level folder that has not been created at the same time than the model ("derived" or "deleted" for instance)
					DBPlugin.debug(DebugLevel.Variable, "Importing first level folder \""+result.getString("name")+"\" (type="+result.getInt("type")+", id="+result.getString("id")+", , parent="+(result.getString("parent")==null?"null":"\""+result.getString("parent")+"\"")+")");
					_dbModel.getFolders().add(folder);
				} else {
					DBPlugin.debug(DebugLevel.Variable, "Importing subfolder \""+result.getString("name")+"\" (type="+result.getInt("type")+", id="+result.getString("id")+", parent="+(result.getString("parent")==null?"null":"\""+result.getString("parent")+"\"")+")");
					dbModel.setSubFolder(result.getString("parent"), folder);
				}
			}
			folder.setId(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(), _dbModel.getVersion()));
			folder.setDocumentation(result.getString("documentation"));
			
			_dbModel.indexEObject(folder);
			
			importObjectProperties(dbModel, folder);
			
			dbTabItem.setCountFolders(++countFolders);
			dbTabItem.setProgressBar(++countTotal);
		}
		result.close();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importFolders()");
	}

	private void importImages(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBImporter.importImages()");
		
		// we import only required images
		for (String path: _dbModel.getImagePaths() ) {
			DBPlugin.debug(DebugLevel.Variable, "Searching in database for image path "+path);
			
			ResultSet result;
			if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
				result = DBPlugin.select(db, "SELECT image FROM images WHERE path = ?", path);
			} else {
				result = DBPlugin.select(db, "MATCH (i:images {path:?}) RETURN i.image as image", path);
			}
			
			if (result.next() ) {
				IArchiveManager archiveMgr = (IArchiveManager)_dbModel.getModel().getAdapter(IArchiveManager.class);
				try {
					String imagePath;
					if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
						imagePath = archiveMgr.addByteContentEntry(path, result.getBytes("image"));
					} else {
						/*
						imagePath = archiveMgr.addByteContentEntry(path, Base64.decode(result.getString("image")));
						*/
						imagePath = archiveMgr.addByteContentEntry(path, Base64.getDecoder().decode(result.getString("image")));
					}
					
				
					if ( imagePath.equals(path) ) {
						DBPlugin.debug(DebugLevel.Variable, "... image imported");
					} else {
						DBPlugin.debug(DebugLevel.Variable, "... image imported but with new path "+imagePath);
						//TODO: the image was already in the cache but with a different path
						//TODO: we must search all the objects with "path" to replace it with "imagepath" 
					}
					dbTabItem.setCountImages(++countImages);
					dbTabItem.setProgressBar(++countTotal);
				} catch (Exception e) {
					throw new Exception("Import of image failed !", e);
				}
			} else {
				throw new Exception("Import of image failed : unkwnown image path "+path);
			}
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBImporter.importImages()");
	}
}
