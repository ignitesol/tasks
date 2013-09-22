"""This module contains routines perform sync via http protocol"""
import logging
import json
import webapp2

from google.appengine.ext import db
from google.appengine.api import users
from datetime import datetime

'''local import'''
import doui_model
from DouiTodoStatuses import DouiTodoStatus
from DouiTodoCategory import DouiTodoCategory
from DouiTodoItem import DouiTodoItem


class Sync(webapp2.RequestHandler):

    SYNC_OBJECTS_DICT = {"DouiTodoStatus": DouiTodoStatus,
                         "DouiTodoCategories": DouiTodoCategory,
                         "DouiTodoItem": DouiTodoItem}

    JSON_REQUEST_PARAM_NAME = "jsonData"
    
    JSON_UPDATED_OBJECT_VALUES = "updateObjectValues"

    JSON_UPDATED_OBJECT_KEY = "dev_updateObjectKey"

    JSON_UPDATED_OBJECT_TIME = "updateObjectTime"

    JSON_UPDATED_OBJECT_TYPE = "updateObjectType"
    
    JSON_LAST_UPDATE_TIMESTAMP = "lastUpdateTimestamp"
    
    JSON_UPDATED_OBJECTS = "updatedObjects"
    
    JSON_REQUEST_TYPE = "requestType"
    JSON_REQUEST_TYPE_GEN_KEYS = "generateKeys"
    JSON_REQUEST_TYPE_UPDATE_DATA = "updateData"
    
    JSON_UPDATE_OBJECT_CLIENT_ID = "client_id"
    
    JSON_UPDATE_ITEM_FK_STATUS = "fk_status"
    JSON_UPDATE_ITEM_FK_CATEGORY = "fk_category"
    
    def get(self):
        self.response.out.write(self.proceedRequest(self.request))
    
    def post(self):
        self.response.out.write(self.proceedRequest(self.request))
        
    def proceedRequest(self, request):
        """This method used to proceed request for update. Obtained HTTP request must contain JSON with data to be updated.
        Received objects will be sync with server database.
        This method will send back a JSON with objects to be updated on the client side. """
        strJsonData = request.get(Sync.JSON_REQUEST_PARAM_NAME)
        result = ""
        logging.info("Received JSON string: " + strJsonData)
        if((None != strJsonData) and (strJsonData != '')):
            requestObject = json.loads(strJsonData)
        else:
            requestObject = {}
            requestObject[Sync.JSON_LAST_UPDATE_TIMESTAMP] = "2000-01-01 00:00:00:00"
            requestObject[Sync.JSON_UPDATED_OBJECTS] = []
            
        if requestObject[Sync.JSON_REQUEST_TYPE] == Sync.JSON_REQUEST_TYPE_GEN_KEYS:
            result = self.generateKeys(requestObject)
        else:
            result = self.proceedRequestObject(requestObject)
        logging.info("Server answer" + result)
        return result 

    def proceedRequestObject(self, requestObject):
        logging.debug("proceedRequestObject( requestObject )")
        clientObjects = {}
        clientObjects["DouiTodoStatus"] = self.updateStatuses(requestObject)
        clientObjects["DouiTodoCategories"] = self.updateCategories(requestObject)
        clientObjects["DouiTodoItem"] =  self.updateItems(requestObject)
        
        serverObjects = self.getServerObjectsAfterLastUpdate(requestObject[Sync.JSON_LAST_UPDATE_TIMESTAMP])
        resultObjects = self.mergeObjects(clientObjects, serverObjects)
        requestObject[Sync.JSON_UPDATED_OBJECTS] = []
             
        values = {}
        values[Sync.JSON_UPDATED_OBJECT_VALUES] = []
        for objectType in resultObjects.keys():
            values[Sync.JSON_UPDATED_OBJECT_VALUES] = []
            values[Sync.JSON_UPDATED_OBJECT_TYPE] = objectType
            for objectValue in resultObjects[objectType]:
                values[Sync.JSON_UPDATED_OBJECT_VALUES].append(objectValue)
            requestObject[Sync.JSON_UPDATED_OBJECTS].append(values.copy())
        requestObject[Sync.JSON_LAST_UPDATE_TIMESTAMP] = datetime.now().strftime("%Y-%m-%d %H:%M:%S:%f")
        return json.dumps(requestObject, cls = doui_model.jsonEncoder)
    
    def getServerObjectsAfterLastUpdate(self, lastUpdateTimestamp):
        """This method return a dictionary of objects which was updated after last device update time"""
        logging.debug("getServerObjectsAfterLastUpdate( lastUpdateTimestamp )")
        result = {};
        for objectType in Sync.SYNC_OBJECTS_DICT.keys():
            result[objectType] = self.getServerObjectsAfterLastUpdateByType(lastUpdateTimestamp, Sync.SYNC_OBJECTS_DICT[objectType])
        return result
            
    def getServerObjectsAfterLastUpdateByType(self, lastUpdateTimestamp, objectModel):
        """ This method returns a dictionary with objects for concrete type, which was updated after last update"""
        result = []
        item = {}
        objectModelQuery = objectModel.all()
        objectModelQuery.filter("updateTimestamp > ", datetime.strptime(lastUpdateTimestamp, "%Y-%m-%d %H:%M:%S:%f"))
        objectModelQuery.filter("userId = ", users.get_current_user().user_id())
        for datastoreObject in objectModelQuery.run():
            item = db.to_dict(datastoreObject)
            logging.info("Application name for key: "+datastoreObject.key().app())
            item[Sync.JSON_UPDATED_OBJECT_KEY] = str(datastoreObject.key())
            item[Sync.JSON_UPDATE_OBJECT_CLIENT_ID] = "null"
            result.append(item.copy())
        return result
        
    def updateStatuses(self, requestObject):
        result = []
        valuesForUpdate = self.getObjectsByType(requestObject, "DouiTodoStatus")
        for value in valuesForUpdate:
            status = DouiTodoStatus(user = users.get_current_user(), userId = users.get_current_user().user_id())
            status.createStatus(value)
            requestItems = self.getObjectsByType(requestObject, "DouiTodoItem")
            for item in requestItems:
                if(item[Sync.JSON_UPDATE_ITEM_FK_STATUS] == value[Sync.JSON_UPDATE_OBJECT_CLIENT_ID]):
                    item[Sync.JSON_UPDATE_ITEM_FK_STATUS] = value[Sync.JSON_UPDATED_OBJECT_KEY]
            result.append(value)
        return result
                
    def updateCategories(self, requestObject):
        result = []
        valuesForUpdate = self.getObjectsByType(requestObject, "DouiTodoCategories")
        for value in valuesForUpdate:
            category = DouiTodoCategory(user = users.get_current_user(), 
                                        userId = users.get_current_user().user_id())
            category.createCategory(value)
            requestItems = self.getObjectsByType(requestObject, "DouiTodoItem")
            for item in requestItems:
                if(item[Sync.JSON_UPDATE_ITEM_FK_CATEGORY] == value[Sync.JSON_UPDATE_OBJECT_CLIENT_ID]):
                    item[Sync.JSON_UPDATE_ITEM_FK_CATEGORY] = value[Sync.JSON_UPDATED_OBJECT_KEY]
            result.append(value)
        return result
        
    def updateItems(self, requestObject):
        result = []
        valuesForUpdate = self.getObjectsByType(requestObject, "DouiTodoItem")
        for value in valuesForUpdate:
            #item = DouiTodoItem(user = users.get_current_user(), userId = users.get_current_user().user_id())
            item = DouiTodoItem.createItem(value)
            result.append(item)
        return result
        
    def getObjectsByType(self, requestObject, objectType):
        result = []
        for updateObject in requestObject[Sync.JSON_UPDATED_OBJECTS]:
            if(updateObject[Sync.JSON_UPDATED_OBJECT_TYPE] == objectType):
                result = updateObject[Sync.JSON_UPDATED_OBJECT_VALUES]
                break
        return result
    
    def mergeObjects(self, clientObjects, serverObjects):
        result = {}
        result["DouiTodoStatus"] = clientObjects["DouiTodoStatus"]
        
        count = 0
        for item in serverObjects["DouiTodoStatus"]:
            if(count < len(clientObjects["DouiTodoStatus"])):
                if(self.isItemExist(item, clientObjects["DouiTodoStatus"]) == 0):
                    result["DouiTodoStatus"].append(item.copy())
                else:
                    ++count
            else:
                result["DouiTodoStatus"].append(item.copy())
        
        result["DouiTodoCategories"] = clientObjects["DouiTodoCategories"]
        count = 0
        for item in serverObjects["DouiTodoCategories"]:
            if(count < len(clientObjects["DouiTodoCategories"])):
                if(self.isItemExist(item, clientObjects["DouiTodoCategories"]) == 0):
                    result["DouiTodoCategories"].append(item.copy())
                else:
                    ++count
            else:
                result["DouiTodoCategories"].append(item.copy())
                
        result["DouiTodoItem"] = clientObjects["DouiTodoItem"]
        for item in serverObjects["DouiTodoItem"]:
            result["DouiTodoItem"].append(item.copy())
        return result
            
    def isItemExist(self, item, listObjects):
        result = 0
        for listItem in listObjects:
            if(listItem[Sync.JSON_UPDATED_OBJECT_KEY] == item[Sync.JSON_UPDATED_OBJECT_KEY]):
                result = 1
                break
        return result
    
    def getItems(self, data):
        result = 0
        for updateObject in data[Sync.JSON_UPDATED_OBJECTS]:
            if(updateObject[Sync.JSON_UPDATED_OBJECT_TYPE] == "DouiTodoItem"):
                result = updateObject
                break
        return result
        
    
    def generateKeys(self, requestObject):
        ''' This method generate new key values for entities received from client. '''
        statusValues = self.getObjectsByType(requestObject, "DouiTodoStatus")
        status = DouiTodoStatus(user = users.get_current_user(), userId = users.get_current_user().user_id())
        for item in statusValues:
            status.generateKeys(item)
        
        categoriesValues = self.getObjectsByType(requestObject, "DouiTodoCategories")
        category = DouiTodoCategory(user = users.get_current_user(), userId = users.get_current_user().user_id())
        for item in categoriesValues:
            category.generateKeys(item)
        
        items = self.getItems(requestObject)
        
        if(items["itemsCount"] > 0):
            DouiTodoItem.generateKeys(items)
        else:
            items["itemsKeys"] = [] 
                
        return  json.dumps(requestObject, cls = doui_model.jsonEncoder)
        
    
        
        
