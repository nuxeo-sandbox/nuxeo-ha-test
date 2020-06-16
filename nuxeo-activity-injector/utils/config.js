const convict = require('convict');
convict.addFormat(require('convict-format-with-validator').url);

// Define a schema
const config = convict({
  env: {
    doc: 'The application environment.',
    format: ['production', 'development', 'test'],
    default: 'development',
    env: 'NODE_ENV',
  },
  primary: {
    doc: 'The primary server URL.',
    format: 'url',
    default: 'http://localhost:8111',
    env: 'PRIMARY_URL',
    arg: 'primary',
  },
  secondary: {
    doc: 'The secondary server URL.',
    format: 'url',
    default: 'http://localhost:8222',
    env: 'SECONDARY_URL',
    arg: 'secondary',
  },
  username: {
    doc: 'Username.',
    format: String,
    default: 'Administrator',
    env: 'USERNAME',
    arg: 'username',
  },
  password: {
    doc: 'Password.',
    format: String,
    default: 'Administrator',
    env: 'PASSWORD',
    arg: 'password',
  },
  all: {
    doc: 'Use all records, not just those with binaries.',
    format: 'Boolean',
    default: false,
    env: 'ALL_RECORDS',
    arg: 'all',
  },
  pageSize: {
    doc: 'Page size for queries.',
    format: 'Number',
    default: 100,
    env: 'PAGE_SIZE',
    arg: 'pageSize',
  },
  outputFile: {
    doc: 'The name of the file to write.',
    format: String,
    default: 'uid_export.csv',
    env: 'OUTPUT_FILE',
    arg: 'outputFile',
  },
  test: {
    doc: 'The test to run',
    format: String,
    default: 'verify',
    env: 'TARGET_TEST',
    arg: 'test',
  },
});

// Load environment dependent configuration
const env = config.get('env');
config.loadFile(`./config/${env}.json`);

// Perform validation
config.validate({
  allowed: 'strict',
});

module.exports = config;
