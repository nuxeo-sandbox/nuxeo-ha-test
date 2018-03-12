const connect = require('./connect.js');
const colors = require('colors/safe');

const allQuery = 'SELECT ecm:uuid, ecm:acl/*/principal, ecm:acl/*/permission, ecm:acl/*/grant, ' +
  'ecm:acl/*/creator, ecm:acl/*/begin, ecm:acl/*/end, ecm:acl/*/name FROM Document';
const queryPageSize = connect.Config.get('pageSize');

const execQuery = allQuery;

function pageExec(client, pageIdx, numberOfPages) {
  console.log(colors.yellow(`Getting page ${pageIdx + 1} of ${numberOfPages}`));
  return client.operation('Repository.ResultSetPageProvider')
    .params({
      query: execQuery,
      language: 'NXQL',
      pageSize: queryPageSize,
      page: pageIdx,
    })
    .execute()
    .then((pdoc) => {
      pdoc.entries.forEach((element) => {
        console.log(JSON.stringify(element, null, 0));
      });
      console.log(colors.green(`Page ${pageIdx} retreived.`));
    })
    .catch((err2) => {
      console.log(colors.red(`Page ${pageIdx} failed - Error executing query: ${err2}`));
    });
}

console.log(colors.yellow('Getting first page...'));
connect.primary.operation('Repository.ResultSetPageProvider')
  .params({
    query: execQuery,
    language: 'NXQL',
    pageSize: queryPageSize,
  })
  .execute()
  .then((doc) => {
    console.log(colors.green(`Retrieved first page, writing ${doc.resultsCount} record(s).`));
    doc.entries.forEach((element) => {
      console.log(JSON.stringify(element, null, 0));
    });
    if (doc.isNextPageAvailable) {
      let pageIdx = 1;
      for (; pageIdx < doc.numberOfPages; pageIdx += 1) {
        pageExec(connect.primary, pageIdx, doc.numberOfPages);
      }
    }
  })
  .catch((error) => {
    console.log(colors.red(`Error executing query:  ${error}`));
  });
