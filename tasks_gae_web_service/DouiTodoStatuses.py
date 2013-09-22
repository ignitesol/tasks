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

class DouiTodoStatus(DouiSyncEntity):
    """Datastorage entity for Doui todo status"""
    name = db.StringProperty()
    lastUpdateTimestamp = db.DateTimeProperty()
    
    JSON_STATUS_KEY = "dev_updateObjectKey"
    JSON_STATUS_UPDATE_TIMESTAMP = "lastUpdateTimestamp"
    JSON_STATUS_NAME = "name"
    
    def createStatus(self, data):
        KeyForUpdate = db.get(data[self.JSON_STATUS_KEY])
        if(KeyForUpdate != None):
            self.updateStatus(KeyForUpdate, data)
        else:
            self._key = Key(data[self.JSON_STATUS_KEY])
            self.loadAttrFromDict(data)
            self.put()
    
    def updateStatus(self, KeyForUpdate, data):
        if(KeyForUpdate.lastUpdateTimestamp < datetime.strptime(data[self.JSON_STATUS_UPDATE_TIMESTAMP], "%Y-%m-%d %H:%M:%S")):
            KeyForUpdate.name = data[self.JSON_STATUS_NAME]
            KeyForUpdate.put() 
        else:
            data[self.JSON_STATUS_NAME] = KeyForUpdate.name
            
    def generateKeys(self, data):
        model = self.all()
        model.filter("name = ", data[self.JSON_STATUS_NAME] )
        model.filter("userId = ", self.userId)
        entity = model.fetch(1)
        if(len(entity) > 0):
            data[self.JSON_STATUS_KEY] = str(entity[0].key())
        else:
            baseKey = db.Key.from_path('DouiTodoStatus', 1)
            ids = db.allocate_ids(baseKey, 1)
            idsRange = (ids[0], ids[1] + 1)
            data[self.JSON_STATUS_KEY] = str(db.Key.from_path('DouiTodoStatus', idsRange[0]))
            
            
        
