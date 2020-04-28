var jwt = require('jsonwebtoken');
var request = require('request');
const vault = require('./vault')

var identity_url = process.env.IdentityUrl
if (!identity_url) {
    throw new Error('Could not find environment variable \'IdentityUrl\'')
}

const respond = function (payload, code) {
    return {
        'statusCode': code,
        'headers': {
            "Access-Control-Allow-Methods": "DELETE,GET,HEAD,OPTIONS,PATCH,POST,PUT",
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Headers": "x-mock-match-request-body,x-action,Authorization,x-sd-emp,Content-Type,Content-Encoding,Accept,Accept-Language,Content-Language,Cache-Control",
            'Content-Type': 'application/json'
        },
        'body': JSON.stringify(payload)
    }
}


var get_emp_token = function (org_id, idToken) {
    return new Promise((resolve, reject) => {
        var options = {
            url: identity_url + "/auth/jwt",
            headers: {
                'x-action': 'jwt',
                'Authorization': `${idToken}`
            },
            method: "POST",
            body: JSON.stringify({
                "OrgId": org_id,
                "jwt_options": {
                    "exp_mins": 10080,
                    "rootUrl_override": false
                }
            })
        };
        request(options, (err, res, body) => {
            console.log(body)
            return resolve(JSON.parse(body).empJwt)
        });
    })
}



const lambda_handler = async (event, context) => {
    if (typeof (event.headers['x-status']) != 'undefined')
        return respond('OK', 200)

    var action = event.headers['x-action']
    var payload = JSON.parse(event.body)
    var session = payload.Session
    var datapoint = payload.DataPoint
    var res = {
        DataPointName: datapoint.DataPointName
    }



    switch (action) {
        case 'pre-allocate-instance':
            const authToken = event.headers['x-sd-emp']
            var org_id = ''
            if (authToken) {
                var decoded = jwt.decode(authToken, { complete: true });
                var empower_web_api = decoded.payload['empower/webApi']
                org_id = decoded.payload['empower/agentId']
                const idToken = event.headers["Authorization"]
                var emp_token = await get_emp_token(org_id, idToken);

                datapoint.Parameters['_callback_headers'] = { 'Authorization': 'Bearer ' + emp_token }
                datapoint.Parameters['_channel'] = 'correspondent'
                datapoint.Parameters['_callback_url'] = empower_web_api
            }

            res.PreAllocated = await vault.pre_allocate_instance(session, datapoint)
            break
        case 'get-instance':
            res.Instance = await vault.get_instance(session, datapoint)
            break
    }
    console.log(res)
    res['Session'] = session
    return respond(res, 200)
};

const options = async (event, context) => {
    return respond('OK', 200)
};

const get = async (event, context) => {
    return respond('OK', 200)
};

module.exports = {
    lambda_handler,
    options,
    respond
}