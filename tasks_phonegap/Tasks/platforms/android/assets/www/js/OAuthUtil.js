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
            apiVersion:"1.0",
            apiName:"task"
        },
        oauthOptions: {
            client_id: '572251750016-qg6jih41a4t765lkakujjh97gf7hdiec.apps.googleusercontent.com',//new client id
            client_secret: 'UwiHI244LpTW1Wb8cHU1UXBZ',//new client secret
            redirect_uri: 'http://localhost',
            scope: 'https://www.googleapis.com/auth/userinfo.email'
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

        isAuthorized: function() {
            var tokenValue = window.localStorage.getItem(this.refreshTokenKey);
            console.log("Refresh Token Value >>" + tokenValue);
            return ((tokenValue !== null) && (typeof tokenValue !== 'undefined'));
        },
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
            this.authWindowRef.addEventListener('loadstart', this._checkAuthCode);
        },
        _checkAuthCode: function (event)
        {
            var url = event.url;
            
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
                            window.localStorage.setItem(oAuthUtil.refreshTokenKey,data.refresh_token);
                            gapi.auth.setToken(data);
                            //window.gce_loaded = true;
                            synchronize();
                        }
                        else
                        {
                            console.log("Get access token error, please login again.");
                        }
                    }, function (err)
                    {
                        console.error("post res error: " + err);
                        console.log("Get access token error: " + err);
                    });
            }
            else if (error)
            {
                console.error("error: " + error[1]);
                console.log("Auth error: " + error[1]);
                if (self.authCallback)
                {
                    self.authCallback({success: false});
                }
            }
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
                        console.log('API Load failed, '+res.error.message);
                    }
                }
                else
                {
                    console.log("API loaded.");
                    if(callback)
                    	callback();
                   	else{
                   	}
                }
            }, this.API.apiRoot);
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
            var self = this;
            try{
                if(!oAuthUtil._isTokenExpired()&&oAuthUtil.accessToken!=null){
                    callback();
                    return;
                }
            }
            catch(e){
              console.log("error = "+e);
            }
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
                    console.log("access token res: " + JSON.stringify(data));
                    oAuthUtil.setAccessToken(data);
                    callback();
                }, function (err)
                {
                    console.error("refresh access token error: " + err);
                    console.log("refresh access token error: " + err);
                    if (errorCallback)
                    {
                        errorCallback();
                    }
                });
        },
        getRefreshToken: function(){
            var rt = window.localStorage.getItem(oAuthUtil.refreshTokenKey);
            console.error("refresh token:" + rt);
            return rt;
        },
        
        getCategoryList: function(callback){
            oAuthUtil.getAccessToken(oAuthUtil.loadAPI(function(){
                var getCategoryList = gapi.client.task.category.list();
                getCategoryList.execute(function (data){
                    if(callback)
                        callback(data);
                });
            }));
        }
    };
    return oAuthUtil;
});