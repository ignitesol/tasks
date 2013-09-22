'''
Created on May 29, 2013

@author: sergey
'''

'''local import'''
from doui_model import DouiSyncEntity
from datetime import datetime

'''GAE import'''
from google.appengine.ext import db
from google.appengine.ext.db import Key

class DouiTodoCategory(DouiSyncEntity):
    """Datastorage entity for Doui todo categories"""
    name = db.StringProperty()
    is_deleted = db.StringProperty()
    lastUpdateTimestamp = db.DateTimeProperty()

    JSON_CATEGORY_KEY = "dev_updateObjectKey"
    JSON_CATEGORY_UPDATE_TIMESTAMP = "lastUpdateTimestamp"
    JSON_CATEGORY_NAME = "name"
    JSON_CATEGORY_IS_DELETED = "is_deleted"

    def createCategory(self, data):
        KeyForUpdate = db.get(data[self.JSON_CATEGORY_KEY])
        if(KeyForUpdate != None):
            self.updateCategory(KeyForUpdate, data)
        else:
            self._key = Key(data[self.JSON_CATEGORY_KEY])
            self.loadAttrFromDict(data)
            self.put()
        
    def updateCategory(self, KeyForUpdate, data):
        if(KeyForUpdate.lastUpdateTimestamp < datetime.strptime(data[self.JSON_CATEGORY_UPDATE_TIMESTAMP], "%Y-%m-%d %H:%M:%S")):
            KeyForUpdate.name = data[self.JSON_CATEGORY_NAME]
            KeyForUpdate.is_deleted = data[self.JSON_CATEGORY_IS_DELETED]
            KeyForUpdate.put() 
        else:
            data[self.JSON_CATEGORY_NAME] = KeyForUpdate.name
            data[self.JSON_CATEGORY_IS_DELETED] = KeyForUpdate.is_deleted
    
    def generateKeys(self, data):
        model = self.all()
        model.filter("name = ", data[self.JSON_CATEGORY_NAME] )
        model.filter("userId = ", self.userId)
        entity = model.fetch(1)
        if(len(entity) > 0):
            data[self.JSON_CATEGORY_KEY] = str(entity[0].key())
        else:
            baseKey = db.Key.from_path('DouiTodoCategory', 1)
            ids = db.allocate_ids(baseKey, 1)
            idsRange = (ids[0], ids[1] + 1)
            data[self.JSON_CATEGORY_KEY] = str(db.Key.from_path('DouiTodoCategory', idsRange[0]))
        