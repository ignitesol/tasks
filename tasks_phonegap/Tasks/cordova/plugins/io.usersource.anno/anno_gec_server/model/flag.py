__author__ = 'topcircler'

from google.appengine.ext import ndb

from model.anno import Anno
from model.base_model import BaseModel
from message.flag_message import FlagMessage
from message.user_message import UserMessage


class Flag(BaseModel):
    """
    Flag data model.
    """
    anno_key = ndb.KeyProperty(kind=Anno)

    def to_message(self):
        """
        Convert Flag data model to flag message.
        """
        message = FlagMessage()
        message.id = self.key.id()
        message.anno_id = self.anno_key.id()
        if self.creator is not None:
            message.creator = self.creator.get().to_message()
        return message

    @classmethod
    def is_belongs_user(cls, anno, user):
        return Flag.query(Flag.anno_key == anno.key, Flag.creator == user.key).get() is not None