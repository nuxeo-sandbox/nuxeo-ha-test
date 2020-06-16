const connect = require('./connect.js');
const colors = require('colors/safe');
const fs = require('fs');


const allQuery = "SELECT * FROM Document WHERE ecm:mixinType != 'HiddenInNavigation' " +
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
  return client.operation('Repository.Query')
    .params({
      query: execQuery,
      language: 'NXQL',
      pageSize: queryPageSize,
      page: pageIdx,
    })
    .execute()
    .then((pdoc) => {
      pdoc.entries.forEach((doc) => {
        let fc = doc.properties;
        if (fc['file:content']) {
          fc = fc['file:content'].digest;
        } else {
          fc = '';
        }
        output.write(`${doc.uid},${fc}\n`);
      });
      console.log(colors.green(`Page ${pageIdx + 1} retrieved.`));
      if (pageIdx + 1 < numberOfPages) {
        pageExec(connect.primary, output, pageIdx + 1, numberOfPages, callback);
      }
    })
    .then(callback)
    .catch((err2) => {
      console.log(colors.red(`Page ${pageIdx} failed - Error executing query: ${err2}`));
    });
}

console.log(colors.yellow('Getting first page...'));
wstream.write('docId,hash\n');
connect.primary.schemas("*");
connect.primary.operation('Repository.Query')
  .params({
    query: execQuery,
    language: 'NXQL',
    pageSize: queryPageSize,
  })
  .execute()
  .then((doc) => {
    console.log(colors.green(`Retrieved first page, writing ${doc.resultsCount} record(s).`));
    doc.entries.forEach((doc) => {
      let fc = doc.properties;
      if (fc['file:content']) {
        fc = fc['file:content'].digest;
      } else {
        fc = '';
      }
      wstream.write(`${doc.uid},${fc}\n`);
    });
    if (doc.isNextPageAvailable) {
      const callback = closeQuery(doc.numberOfPages - 1);
      pageExec(connect.primary, wstream, 1, doc.numberOfPages, callback);
    }
  })
  .catch((error) => {
    console.log(colors.red(`Error executing query:  ${error}`));
  });
