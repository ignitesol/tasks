'''
Created on 03.11.2013

@author: Sergey Gadzhilov
'''

import endpoints

from protorpc import remote, message_types, messages
from task_messages import CategoryRequest, TaskRequest, TaskResponse,\
    CategoryListResponse, DeleteRequest, CategoryUpdateRequest,\
    TaskListResponse, TaskUpdateStatusRequest, TaskUpdateRequest
from task_messages import CategoryResponse
from model.Category import Category
from model.Task import Task 
from model.User import User 
from google.appengine.ext import ndb
from google.appengine.datastore.datastore_query import Cursor
from google.appengine.ext.db import BadValueError
import Settings

CLIENT_ID = '290040807346.apps.googleusercontent.com'


@endpoints.api(name="task", 
               version="v1",
               description="Task api", 
               allowed_client_ids=[CLIENT_ID, endpoints.API_EXPLORER_CLIENT_ID])
class TaskApi(remote.Service):
    
    @endpoints.method( CategoryRequest, 
                       CategoryResponse,
                       path = 'category', 
                       http_method = 'POST',
                       name = 'category.insert')
    def CreateCategory(self, request):
        category = Category(name = request.name, user = self.GetUserId())
        category.put()
        return category.ConvertToResponse()
    
    CategoryListRequest = endpoints.ResourceContainer(
        message_types.VoidMessage,
        cursor = messages.StringField(2),
        limit = messages.IntegerField(3)
    )
    
    @endpoints.method( CategoryListRequest, 
                       CategoryListResponse,
                       path = 'category', 
                       http_method = 'GET',
                       name = 'category.list')
    def GetCategoryList(self, request):
        
        limit = 10
        if request.limit is not None:
            limit = request.limit
        
        curs = None
        if request.cursor is not None:
            try:
                curs = Cursor(urlsafe=request.cursor)
            except BadValueError:
                raise endpoints.BadRequestException('Invalid cursor %s.' % request.cursor)
     
        if (curs is not None):
            categories, next_curs, more = Category.query(Category.user == self.GetUserId()).fetch_page(limit, start_cursor=curs)
        else:
            categories, next_curs, more = Category.query(Category.user == self.GetUserId()).fetch_page(limit)
            
        items = [entity.ConvertToResponse() for entity in categories]

        if more:
            return CategoryListResponse(category_list=items, cursor=next_curs.urlsafe(), has_more=more)
        else:
            return CategoryListResponse(category_list=items, has_more=more)
    
    @endpoints.method( DeleteRequest,
                       CategoryResponse,
                       path = 'category.delete', 
                       http_method = 'POST',
                       name = 'category.delete')
    def DeleteCategory(self, request):
        result = CategoryResponse()
        category = Category.get_by_id(request.id)
        
        if category != None:
            if Task.query(Task.category == category.key).get() == None:
                if category.last_updated <= request.client_copy_timestamp and category.user == self.GetUserId():
                    category.key.delete()
                    result = category.ConvertToResponse()
                else:
                    raise endpoints.NotFoundException("The item was updated on the outside")
            else:
                raise endpoints.NotFoundException("This item has child elements") 
        else:
            raise endpoints.NotFoundException('No category entity with the id "%s" exists.' % request.id)
        
        return result
    
    @endpoints.method( CategoryUpdateRequest,
                       CategoryResponse,
                       path = 'category.update', 
                       http_method = 'POST',
                       name = 'category.update')
    def UpdateCategoryName(self, request):
        result = CategoryResponse()
        category = Category.get_by_id(request.id)
        if category != None:
            if category.last_updated <= request.client_copy_timestamp and category.user == self.GetUserId():
                category.name = request.new_name
                category.put()
                result = category.ConvertToResponse()
            else:
                raise endpoints.NotFoundException("The item was updated on the outside") 
        else:
            raise endpoints.NotFoundException('No category entity with the id "%s" exists.' % request.id)
        return result
        
    
    @endpoints.method( TaskRequest, 
                       TaskResponse,
                       path = 'task', 
                       http_method = 'POST',
                       name = 'insert')
    def CreateTask(self, request):
        CategoryKey = ndb.Key('Category', request.category)
        if CategoryKey == None:
            raise endpoints.NotFoundException('No category entity with the id "%s" exists.' % request.category)
        
        task = Task(status = request.status,
                    category= CategoryKey,
                    title = request.title,
                    description = request.description,
                    user = self.GetUserId())
        task.put()
        return task.ConvertToResponse()
     
    TaskListRequest = endpoints.ResourceContainer(
        message_types.VoidMessage,
        timestamp = message_types.DateTimeField(2),
        cursor = messages.StringField(3),
        limit = messages.IntegerField(4)
    )
     
    @endpoints.method( TaskListRequest, 
                       TaskListResponse,
                       path = 'task', 
                       http_method = 'GET',
                       name = 'list')
    def GetTaskList(self, request):
        limit = 10
        if request.limit is not None:
            limit = request.limit
        
        curs = None
        if request.cursor is not None:
            try:
                curs = Cursor(urlsafe=request.cursor)
            except BadValueError:
                raise endpoints.BadRequestException('Invalid cursor %s.' % request.cursor)
        
        hQuery = None
        if request.timestamp is not None:
            hQuery = Task.query(Task.last_updated <= request.timestamp, Task.user == self.GetUserId())
        else:
            hQuery = Task.query(Task.user == self.GetUserId())
     
        if (curs is not None):
            tasks, next_curs, more = hQuery.fetch_page(limit, start_cursor=curs)
        else:
            tasks, next_curs, more = hQuery.fetch_page(limit)
            
        items = [entity.ConvertToResponse() for entity in tasks]

        if more:
            return TaskListResponse(task_list=items, cursor=next_curs.urlsafe(), has_more=more)
        else:
            return TaskListResponse(task_list=items, has_more=more)
    
    
    GetTaskRequest = endpoints.ResourceContainer(
        message_types.VoidMessage,
        id=messages.IntegerField(2, required=True)
    )
    
    @endpoints.method(GetTaskRequest, 
                      TaskResponse,
                      path = 'task/{id}', 
                      http_method = 'GET',
                      name = 'get')
    def GetTaskById(self, request):
        if request.id is None:
            raise endpoints.BadRequestException('id field is required.')
        task = Task.get_by_id(request.id)
        if task is None or task.user != self.GetUserId():
            raise endpoints.NotFoundException('No task entity with the id "%s" exists.' % request.id)
        return task.ConvertToResponse()
    
    @endpoints.method( TaskUpdateStatusRequest,
                       TaskResponse,
                       path = 'task.status.update', 
                       http_method = 'POST',
                       name = 'status.update')
    def UpdateTaskStatus(self, request):
        result = TaskResponse()
        task = Task.get_by_id(request.id)
        if task != None and task.user == self.GetUserId():
            if task.last_updated <= request.client_copy_timestamp:
                task.status = request.new_status
                task.put()
                result = task.ConvertToResponse()
            else:
                raise endpoints.NotFoundException("The item was updated on the outside") 
        else:
            raise endpoints.NotFoundException('No task entity with the id "%s" exists.' % request.id)
        return result
    
    @endpoints.method( TaskUpdateRequest,
                       TaskResponse,
                       path = 'task.update',
                       http_method = 'POST',
                       name = 'update')
    def UpdateTask(self, request):
        result = TaskResponse()
        task = Task.get_by_id(request.id)
        if task != None and task.user == self.GetUserId():
            if task.last_updated <= request.client_copy_timestamp:
                task.MergeFromMessage(request)
                task.put()
                result = task.ConvertToResponse()
            else:
                raise endpoints.NotFoundException("The item was updated on the outside")
        else:
            raise endpoints.NotFoundException('No task entity with the id "%s" exists.' % request.id)
        return result

    def GetUserId(self):
        
        if endpoints.get_current_user() == None:
            raise endpoints.UnauthorizedException("Must log in")
        
        user = User.query(User.username == endpoints.get_current_user().email()).get()
        if user == None:
            user = User(username = endpoints.get_current_user().email())
            user.put()
            self.CreateDefaultCategories()
        return user.key

    def CreateDefaultCategories(self):
        for category in Settings.default_categories:
            category = Category(name = category, user = self.GetUserId())
            category.put()

application = endpoints.api_server(api_services=[TaskApi], restricted=False)        