""" This module intended to provide Web GUI to access data from the Doui 
application cloud storage"""

import jinja2
import os
import webapp2
import sync
from ReverseProxyServer import ReverseProxyServer

jinja_environment = jinja2.Environment(
    loader = jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions = ['jinja2.ext.autoescape'])

class WebGui(webapp2.RequestHandler):
    def get(self):
        jinja_environment.get_template('index.html')

application = webapp2.WSGIApplication([
    ('/', WebGui),
    ('/sync', sync.Sync),
], debug = True)

application = ReverseProxyServer(application)
