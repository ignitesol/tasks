'''
Created on 02.11.2013

@author: Sergey Gadzhilov
'''
from google.appengine.ext import ndb
from task_messages import TaskResponse
from model.Category import Category
import endpoints

class Task(ndb.Model):
    user = ndb.KeyProperty(kind="User", indexed=True)
    status = ndb.StringProperty()
    category = ndb.KeyProperty(kind="Category", indexed=True)
    title = ndb.StringProperty()
    description = ndb.StringProperty()
    last_updated = ndb.DateTimeProperty(auto_now=True, indexed=True)
    
    def ConvertToResponse(self):
        objCategory = self.category.get()
        if objCategory == None:
            raise endpoints.NotFoundException('No category entity with the id "%s" exists.' % self.category.id())

        return TaskResponse(id = self.key.id(),
                            user = 0,
                            status = self.status,
                            category = objCategory.name,
                            title = self.title,
                            description = self.description,
                            last_updated = self.last_updated)

    def MergeFromMessage(self, message):
        if message.status is not None:
            self.status = message.status

        if message.category is not None:
            self.category = Category.get_by_id(message.category).key

        if message.title is not None:
            self.title = message.title

        if message.description is not None:
            self.description = message.description
