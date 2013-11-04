'''
Created on 02.11.2013

@author: Sergey Gadzhilov
'''
from google.appengine.ext import ndb
from task_messages import TaskResponse

class Task(ndb.Model):
    user = ndb.KeyProperty(kind="User", indexed=True)
    status = ndb.StringProperty()
    category = ndb.KeyProperty(kind="Category", indexed=True)
    title = ndb.StringProperty()
    description = ndb.StringProperty()
    last_updated = ndb.DateTimeProperty(auto_now=True, indexed=True)
    
    def ConvertToResponse(self):
        return TaskResponse(id = self.key.id(),
                            user = 0,
                            status = self.status,
                            category = long(self.category.id()),
                            title = self.title,
                            description = self.description,
                            last_updated = self.last_updated)
    