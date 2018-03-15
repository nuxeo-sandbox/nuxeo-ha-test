const connect = require('./connect.js');
const colors = require('colors/safe');
const fs = require('fs');


const allQuery = "SELECT ecm:uuid, file:content/data FROM Document WHERE ecm:mixinType != 'HiddenInNavigation' " +
  " AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted'";
const fileQuery = `${allQuery} AND file:content/data IS NOT NULL`;
const queryPageSize = connect.Config.get('pageSize');

let execQuery = fileQuery;
if (connect.Config.get('all')) {
  execQuery = allQuery;
}
const wstream = fs.createWriteStream(connect.Config.get('outputFile'));

function closeQuery(pageCount) {
  let remaining = pageCount;

  function barrier() {
    remaining -= 1;
    if (remaining === 0) {
      console.log(colors.bgGreen('Done.'));
      wstream.close();
    } else {
      console.log(colors.blue(`${remaining} page(s) left`));
    }
  }
  return barrier;
}

function pageExec(client, output, pageIdx, numberOfPages, callback) {
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
        output.write(`${element['ecm:uuid']},${element['file:content/data']}\n`);
      });
      console.log(colors.green(`Page ${pageIdx} retreived.`));
    })
    .then(callback)
    .catch((err2) => {
      console.log(colors.red(`Page ${pageIdx} failed - Error executing query: ${err2}`));
    });
}

console.log(colors.yellow('Getting first page...'));
wstream.write('docId,hash\n');
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
      wstream.write(`${element['ecm:uuid']},${element['file:content/data']}\n`);
    });
    if (doc.isNextPageAvailable) {
      let pageIdx = 1;
      const callback = closeQuery(doc.numberOfPages - 1);
      for (; pageIdx < doc.numberOfPages; pageIdx += 1) {
        pageExec(connect.primary, wstream, pageIdx, doc.numberOfPages, callback);
      }
    }
  })
  .catch((error) => {
    console.log(colors.red(`Error executing query:  ${error}`));
  });
