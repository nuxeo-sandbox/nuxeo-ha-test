const connect = require('./connect.js');
const colors = require('colors/safe');

const execQuery = "SELECT * FROM File WHERE ecm:mixinType != 'HiddenInNavigation' " +
  " AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted' " +
  " AND dc:description != 'updated' ";
const queryPageSize = connect.Config.get('pageSize');

console.log(colors.yellow('Executing query...'));
connect.primary.operation('Repository.Query')
  .params({
    query: execQuery,
    language: 'NXQL',
    pageSize: queryPageSize,
  })
  .execute()
  .then((doc) => {
    console.log(colors.green(`Found ${doc.resultsCount} record(s).`));
  })
  .catch((error) => {
    console.log(colors.red(`Error executing query:  ${error}`));
  });
