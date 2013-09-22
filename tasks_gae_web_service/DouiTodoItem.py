'''
Created on May 29, 2013

@author: sergey
'''
import logging
'''local import'''
from doui_model import DouiSyncEntity
from datetime import datetime

'''GAE import'''
from google.appengine.ext import db
from google.appengine.ext.db import Key
from google.appengine.api import users

class DouiTodoItem(DouiSyncEntity):
    """Datastorage entity for Doui todo item"""
    title = db.StringProperty()
    body = db.TextProperty()
    fk_category = db.StringProperty()
    fk_status = db.StringProperty()
    lastUpdateTimestamp = db.DateTimeProperty()
    
    JSON_ITEM_KEY = "dev_updateObjectKey"
    JSON_ITEM_UPDATE_TIMESTAMP = "lastUpdateTimestamp"
    JSON_ITEM_TITLE = "title"
    JSON_ITEM_BODY = "body"
    JSON_ITEM_CATEGORY = "fk_category"
    JSON_ITEM_STATUS = "fk_status"

    def updateItem(self, data):
        '''THis method updates item or JSON packet properties in depend of last update timestamp.
        If existent item was updated before the received data, data will be overwrite by item values,
        otherwise item will be updated with data values. '''
        if(self.lastUpdateTimestamp < datetime.strptime(data[self.JSON_ITEM_UPDATE_TIMESTAMP], "%Y-%m-%d %H:%M:%S")):
            self.title = data[self.JSON_ITEM_TITLE]
            self.body = data[self.JSON_ITEM_BODY]
            self.fk_category = data[self.JSON_ITEM_CATEGORY]
            self.fk_status = data[self.JSON_ITEM_STATUS]
            self.put()
        else:
            data[self.JSON_ITEM_TITLE] = self.title
            data[self.JSON_ITEM_BODY] = self.body
            data[self.JSON_ITEM_CATEGORY] = self.fk_category 
            data[self.JSON_ITEM_STATUS] = self.fk_status 
            self.put()
          
    @classmethod  
    def generateKeys(cls, data):
        logging.debug("generateKeys() started")
        data["itemsKeys"] = []
        
        baseKey = db.Key.from_path('DouiTodoCategory', 1)
        ids = db.allocate_ids(baseKey, data["itemsCount"])
        idsRange = range(ids[0], ids[1] + 1)
        
        for item in range(0, data["itemsCount"]):
            strNewKey = str(db.Key.from_path('DouiTodoItem', idsRange[item]))
            logging.debug("Created key: "+strNewKey+", for id: "+str(idsRange[item]))
            data["itemsKeys"].append(strNewKey)
        logging.debug("generateKeys() finished")

    @classmethod
    def createItem(cls, data):
        logging.debug("Key value for update: " + str(data[DouiTodoItem.JSON_ITEM_KEY]))
        itemForUpdate = DouiTodoItem.get(data[DouiTodoItem.JSON_ITEM_KEY])
        if(itemForUpdate != None):
            itemForUpdate.updateItem(data)
        else:
            
            itemForUpdate = DouiTodoItem(key=data[DouiTodoItem.JSON_ITEM_KEY], user = users.get_current_user(), userId = users.get_current_user().user_id())
            itemForUpdate.loadAttrFromDict(data)
            itemForUpdate.put()
        logging.debug("Current key value: " + str(itemForUpdate.key()))            