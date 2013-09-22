'''
Created on May 6, 2013

@author: rsh
'''
import unittest
import urllib
from sync import Sync
import json
import cookielib
import urllib2


class TestSyncRequests(unittest.TestCase):

    GAE_URL = "http://127.0.0.1:8080"
    
    LOGIN_URL = GAE_URL + "/_ah/login"
    
    def setUp(self):
        unittest.TestCase.setUp(self)
        cookiejar = cookielib.CookieJar()
        self.opener = urllib2.build_opener(urllib2.HTTPCookieProcessor(cookiejar))
        urllib2.install_opener(self.opener)       
        authreq_data = urllib.urlencode({'email': 'test2@example.com',
                                         'action': 'Login'})
        auth_req = urllib2.Request(TestSyncRequests.LOGIN_URL + "?" + authreq_data)
        self.opener.open(auth_req)

    def testSendNotExistenObjectsForUpdate(self):
            
        dictUpdateObjects = {}
        dictUpdateObjects[Sync.JSON_LAST_UPDATE_TIMESTAMP] = "2011-06-21 16:30:00"
        dictUpdateObjects[Sync.JSON_UPDATED_OBJECTS] = []
        
        dictObjSync = {}
        dictObjSync[Sync.JSON_UPDATED_OBJECT_TYPE] = "DouiTodoItem"
        dictObjSync[Sync.JSON_UPDATED_OBJECT_VALUES] = [] 
        dictObjSync[Sync.JSON_UPDATED_OBJECT_VALUES].append({"title":"someTitle", "body":"some text in description (body)", "updateObjectTime":"21/11/06 17:30", "updateObjectKey":None})
        
        dictUpdateObjects[Sync.JSON_UPDATED_OBJECTS].append(dictObjSync)
        
        strJson = json.dumps(dictUpdateObjects)
                
        params = urllib.urlencode({Sync.JSON_REQUEST_PARAM_NAME:strJson})
        
        data_req = urllib2.Request(TestSyncRequests.GAE_URL + "/sync")
        data_req.add_data(params)
        self.opener.open(data_req)
        
    def testCallSyncWithoutParametrs(self):
        data_req = urllib2.Request(TestSyncRequests.GAE_URL + "/sync")
        self.opener.open(data_req)
        
        
        

if __name__ == "__main__":
    # import sys;sys.argv = ['', 'Test.testName']
    unittest.main()
