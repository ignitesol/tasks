'''
Created on 03.11.2013

@author: Sergey Gadzhilov
'''

from task_messages import CategoryResponse
from google.appengine.ext import ndb

class Category(ndb.Model):
    user = ndb.KeyProperty(kind="User", indexed=True)
    last_updated = ndb.DateTimeProperty(auto_now=True)
    name = ndb.StringProperty()
    
    def ConvertToResponse(self):
        return CategoryResponse(id = self.key.id(),
                                last_updated=self.last_updated,
                                name = self.name,
                                user = self.user.id())