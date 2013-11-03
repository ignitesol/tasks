'''
Created on 03.11.2013

@author: Sergey Gadzhilov
'''

from google.appengine.ext import ndb


class User(ndb.Model):
    username = ndb.StringProperty()
    password = ndb.StringProperty()
    last_updated = ndb.DateTimeProperty(auto_now=True)
