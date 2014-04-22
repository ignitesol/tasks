__author__ = 'topcircler'

import endpoints

from api.vote_api import VoteApi
from api.anno_api import AnnoApi
from api.flag_api import FlagApi
from api.followup_api import FollowupApi
from api.user_api import UserApi
from api.account_api import AccountApi

APPLICATION = endpoints.api_server([VoteApi, AnnoApi, FlagApi, FollowupApi, UserApi, AccountApi], restricted=False)