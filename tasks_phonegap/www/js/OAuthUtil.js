/**
 * Created by Leonid on 12/23/13.
 */
define([
    "dojo/request/xhr"
], function (xhr)
{
    var oAuthUtil = {
        API:{
            apiRoot:"https://usersource-tasks-test.appspot.com/_ah/api",
            //apiRoot: "https://annoserver-test.appspot.com/_ah/api",
            apiVersion:"1.0",
            apiName:"task"
            //apiName: "anno"
        },
        oauthOptions: {
        	client_id: '572251750016-in9jr98erjli2rqfgcpmjrc29202tv2a.apps.googleusercontent.com',//new client id
            client_secret: '5Yv6ZyPPK9P3HNhFIUwl8WFa',//new client secret
            //client_id : "394023691674-7j5afcjlibblt47qehnsh3d4o931orek.apps.googleusercontent.com",
            //client_secret: "n0fJeoZ-4UFWZaIG41mNg41_",
            redirect_uri: 'http://localhost:63342/www/oauthcallback.html',
            scope: 'https://www.googleapis.com/auth/userinfo.email'
            //scope: 'https://www.googleapis.com/auth/tasks'
        },
        grantTypes: {
            AUTHORIZE: "authorization_code",
            REFRESH: "refresh_token"
        },
        authUrl: "https://accounts.google.com/o/oauth2/auth",
        tokenUrl: "https://accounts.google.com/o/oauth2/token",
        authWindowRef: null,
        checkingAuthCode:false,
        refreshTokenKey: "refresh_token",
        accessToken:null,
        accessTokenTime:0,
        accessTokenExpiryLimit:58 * 60 * 1000,
        openAuthWindow: function (authCallback)
        {
            this.authCallback = authCallback;
            var url = this.authUrl +
                "?client_id=" + this.oauthOptions.client_id +
                "&redirect_uri=" + this.oauthOptions.redirect_uri +
                "&response_type=code" +
                "&origin=http://localhost:8080" +
                "&access_type=offline" +
                "&approval_prompt=force" +
                "&scope=" + this.oauthOptions.scope;

            this.checkingAuthCode = false;
            this.authWindowRef = window.open(url, '_blank', 'location=no');
            self = this;
            //this.authWindowRef.addEventListener('loadstart', this._checkAuthCode);
            //this.authWindowRef.addEventListener('loadstop', function(e){alert("load stop");});
            /*this.authWindowRef.onbeforeunload = function(event) {
                oAuthUtil._checkAuthCode();
            }*/
        },
        _checkAuthCode: function (event)
        {
            var url = event.url;alert(url);
            var code = /\?code=(.+)$/.exec(url);
            var error = /\?error=(.+)$/.exec(url);

            if (code || error)
            {
                //Always close the browser when match is found
                console.error(code[1]);
                oAuthUtil.authWindowRef.close();
            }

            if (code)
            {
                if (this.checkingAuthCode) return;
                this.checkingAuthCode = true;

                var self = this;
                //Exchange the authorization code for an access token
                xhr.post(oAuthUtil.tokenUrl, {
                    data: {
                        code: code[1],
                        client_id: oAuthUtil.oauthOptions.client_id,
                        client_secret: oAuthUtil.oauthOptions.client_secret,
                        redirect_uri: oAuthUtil.oauthOptions.redirect_uri,
                        grant_type: oAuthUtil.grantTypes.AUTHORIZE
                    },
                    handleAs: "json"
                }).then(function (data)
                    {
                        console.error("post res: " + JSON.stringify(data));

                        if (data&&data.refresh_token)
                        {
                            //self._saveRefreshToken(data);
                            //if (_hasUserInLocalDB)
                            //{
                            //    if (self.authCallback)
                            //    {
                            //        self.authCallback({success: true, token: data});
                            //    }
                            //}
                            //else
                            //{
                            //alert(data.refresh_token);
                            window.localStorage.setItem(oAuthUtil.refreshTokenKey,data.refresh_token);
                                gapi.auth.setToken(data);
                                /*gapi.auth.setToken({
									access_token: data.access_token
								});*/
                                oAuthUtil.getUserInfo(data);
                            //}
                        }
                        else
                        {
                            alert("Get access token error, please login again.");
                            if (self.authCallback)
                            {
                                self.authCallback({success: false});
                            }
                        }
                    }, function (err)
                    {
                        console.error("post res error: " + err);
                        alert("Get access token error: " + err);
                        if (self.authCallback)
                        {
                            self.authCallback({success: false});
                        }
                    });
            }
            else if (error)
            {
                console.error("error: " + error[1]);
                alert("Auth error: " + error[1]);
                if (self.authCallback)
                {
                    self.authCallback({success: false});
                }
            }
        },
        getUserInfo: function(token)
        {
            var self = this;
            gapi.client.load('oauth2', 'v2', function() {
                console.log("oauth loaded");
                oAuthUtil.getAccessToken(function(){
			  		oAuthUtil.loadAPI(oAuthUtil.getCategoryList);
			  	});
                var request = gapi.client.oauth2.userinfo.get();
                request.execute(function(userinfo){
                    console.log("get userinfo res: "+ userinfo);
                    if (userinfo.error)
                    {
                        //alert("Get userinfo failed: "+ userinfo.error.message);
                        return;
                    }

                    //currentUserInfo = userinfo;
                    

                    if (self.authCallback)
                    {
                        self.authCallback({success:true, userInfo:userinfo, token:token});
                    }
                });
            });
        },
        loadAPI: function(callback, errorCallback){
            gapi.client.load(this.API.apiName, 'v1', function(res) {
                if (res&&res.error)
                {
                    console.log("API load failed.");

                    if (errorCallback)
                    {
                        errorCallback();
                    }
                    else
                    {
                        alert('API Load failed, '+res.error.message);
                    }
                }
                else
                {
                    //alert("API loaded.");
                    callback();
                }
            }, this.API.apiRoot);
        },
        init: function(){alert("init");
            var apisToLoad;
            var self = this;
            var callback = function() {
                if (--apisToLoad == 0){try{ 
                    oAuthUtil.signIn(true, oAuthUtil.userAuthed);
                    }catch(e){alert(e);}
                }
            }

            apisToLoad = 2; // must match number of calls to gapi.client.load()
            gapi.client.load(oAuthUtil.API.apiName, 'v1', callback, oAuthUtil.API.apiRoot);
            gapi.client.load('oauth2', 'v2', callback);
        },
        userAuthed: function(){alert("auth...");
        	var self = this;
            var request = gapi.client.oauth2.userinfo.get().execute(function(resp) {
                if (!resp.code) {
                    // User is signed in, call my Endpoint
                    alert("User authed");
                    self.getCategoryList();
                }
                else{
                	alert(resp.code);
                }
            });
        },
        signIn: function(mode, callback){try{alert("signing in...");
            gapi.auth.authorize({client_id: oAuthUtil.oauthOptions.client_id,
                    scope: oAuthUtil.oauthOptions.scope, immediate: mode},
                callback);}catch(e){alert(e);}
        },
        _isTokenExpired: function()
        {
            var currentTime = (new Date()).getTime();

            return !(currentTime < (this.accessTokenTime + this.accessTokenExpiryLimit));
        },
        setAccessToken: function(tokenObject)
        {
            this.accessToken = tokenObject;
            this.accessTokenTime = (new Date()).getTime();
            console.error(gapi.auth);
            console.log("access token res: " + JSON.stringify(tokenObject));
            gapi.auth.setToken(tokenObject);
        },
        getAccessToken:function(callback, errorCallback)
        {
            //var userInfo = annoUtil.getCurrentUserInfo();

            /*if (userInfo.signinmethod == this.signinMethod.anno)
            {
                callback();
                return;
            }*/

            if (this.accessToken&&!this._isTokenExpired())
            {
                callback();
                return;
            }

            var self = this;
            //Exchange the refresh token for an access token
            xhr.post(oAuthUtil.tokenUrl, {
                data: {
                    client_id: oAuthUtil.oauthOptions.client_id,
                    client_secret: oAuthUtil.oauthOptions.client_secret,
                    refresh_token: oAuthUtil.getRefreshToken(),
                    grant_type: oAuthUtil.grantTypes.REFRESH
                },
                handleAs: "json"
            }).then(function (data)
                {
                    //alert("access token res: " + JSON.stringify(data));
                    oAuthUtil.setAccessToken(data);

                    callback();
                }, function (err)
                {
                    console.error("refresh access token error: " + err);
                    alert("refresh access token error: " + err);
                    if (errorCallback)
                    {
                        errorCallback();
                    }
                    else
                    {
                        //annoUtil.hideLoadingIndicator();
                    }
                });
        },
        getRefreshToken: function(){
            var rt = window.localStorage.getItem(oAuthUtil.refreshTokenKey);
            console.error("refresh token:" + rt);
            return rt;
        },
        getCategoryList: function(){
        	var token = gapi.auth.getToken();
        	console.log(JSON.stringify(token));
        	var getCategoryList = gapi.client.task.list();
        	//var getCategoryList = gapi.client.tasks.tasks.list({tasklist: '@default'});
        	getCategoryList.execute(function (data){
        		console.log(JSON.stringify(data));
            });
            /*var arg = {outcome: 'cursor,has_more,anno_list', limit: 30};
			var getAnnoList = gapi.client.anno.anno.list(arg);
			getAnnoList.execute(function (data){ console.error(JSON.stringify(data)); });*/         
        }
    };
    return oAuthUtil;
});