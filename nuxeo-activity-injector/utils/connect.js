const Nuxeo = require('nuxeo');
const Config = require('./config.js');

exports.primary = new Nuxeo({
  baseURL: `${Config.get('primary')}/nuxeo/`,
  auth: {
    method: 'basic',
    username: `${Config.get('username')}`,
    password: `${Config.get('password')}`,
  },
});

exports.secondary = new Nuxeo({
  baseURL: `${Config.get('secondary')}/nuxeo/`,
  auth: {
    method: 'basic',
    username: `${Config.get('username')}`,
    password: `${Config.get('password')}`,
  },
});

exports.Config = Config;
