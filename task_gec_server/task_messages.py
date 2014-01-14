'''
Created on 03.11.2013

@author: Sergey Gadzhilov
'''

from protorpc import messages
from protorpc import message_types


class CategoryRequest(messages.Message):
    name = messages.StringField(1, required=True)
    
class CategoryResponse(messages.Message):
    id = messages.IntegerField(1)
    last_updated = message_types.DateTimeField(2)
    name = messages.StringField(3)
    user = messages.IntegerField(4)
    
class CategoryListResponse(messages.Message):
    category_list = messages.MessageField(CategoryResponse, 1, repeated=True)
    cursor = messages.StringField(2)
    has_more = messages.BooleanField(3)
    
class CategoryUpdateRequest(messages.Message):
    id = messages.IntegerField(1, required=True)
    client_copy_timestamp = message_types.DateTimeField(2, required=True)
    new_name = messages.StringField(3, required=True)
           
class TaskRequest(messages.Message):
    status = messages.StringField(1)
    category = messages.IntegerField(2)
    title = messages.StringField(3)
    description = messages.StringField(4)
    
class TaskResponse(messages.Message):
    id = messages.IntegerField(1)
    user = messages.IntegerField(2)
    status = messages.StringField(3)
    category = messages.StringField(4)
    title = messages.StringField(5)
    description = messages.StringField(6)
    last_updated = message_types.DateTimeField(7)
    
class TaskListResponse(messages.Message):
    task_list = messages.MessageField(TaskResponse, 1, repeated=True)
    cursor = messages.StringField(2)
    has_more = messages.BooleanField(3)
    
class TaskUpdateStatusRequest(messages.Message):
    id = messages.IntegerField(1, required=True)
    client_copy_timestamp = message_types.DateTimeField(2, required=True)
    new_status = messages.StringField(3, required=True)
    
class DeleteRequest(messages.Message):
    id = messages.IntegerField(1, required=True)
    client_copy_timestamp = message_types.DateTimeField(2, required=True)

class TaskUpdateRequest(messages.Message):
    id = messages.IntegerField(1, required=True)
    client_copy_timestamp = message_types.DateTimeField(2, required=True)
    status = messages.StringField(3)
    category = messages.IntegerField(4)
    title = messages.StringField(5)
    description = messages.StringField(6)
