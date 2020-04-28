const fs = require("fs");
const path = require("path");
const jwt = require("jsonwebtoken");
var AWS = require("aws-sdk");
var moment = require("moment");
const request = require("request");
var ssm = new AWS.SSM({region: 'us-east-1',});

if (!process.env.JWTPrivateKey) {
  throw new Error('Could not find environment variable \'JWTPrivateKey\'')
}
// if (!process.env.JWTIssuer) {
//   throw new Error('Could not find environment variable \'JWTIssuer\'')
// }
// if (!process.env.JWTAudience) {
//   throw new Error('Could not find environment variable \'JWTAudience\'')
// }


const createVaultJwt = async (rootUrl, options) => {
  var options = options || {
      exp_mins:15,
      rootUrl_override:false
    }
  rootUrl = options.rootUrl_override ? options.rootUrl_override : rootUrl
  console.log("rootUrl: ", rootUrl);

  //  var params =  await  ssm.getParameters({ Names: [process.env.JWTPrivateKey], WithDecryption: true }).promise();
  
  var cert = process.env.JWTPrivateKey

  const currTime = moment();
  let fromNow = moment();
  fromNow = fromNow.add(options.exp_mins, "minutes");

  // const name = profile.FirstName + " " + profile.LastName
    // console.log('profile: ', profile)
  let payload = {
    // sub: profile.Org.AccountId,
    sub: "Test",
    iat: currTime.unix(),
    // iss: process.env.JWTIssuer,
    iss: "sd-dev.heavywater.com",
    // aud: process.env.JWTAudience,
    aud: "vault.sellerdigital.bkiclouddev.com",

    exp: fromNow.unix(),
    // "name": name,
    // "user": profile.Email
    "user": "goutham.budda@bkfs.com",
    "client": "942"
  };

  const token = jwt.sign(payload, cert, { algorithm: "RS256" });
  console.log("token: ", token);
  return token;
};


module.exports = {
    createVaultJwt
}
