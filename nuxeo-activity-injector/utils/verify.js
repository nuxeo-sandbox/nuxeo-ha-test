const connect = require('./connect.js');
const colors = require('colors/safe');


const query = "SELECT * FROM Document WHERE ecm:mixinType != 'HiddenInNavigation' " +
  " AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted'";

connect.primary.repository().schemas(['dublincore']).query({
  query,
}).then((docs) => {
  console.log(colors.gray(`Primary Count: ${docs.resultsCount}`));
  connect.secondary.repository().schemas(['dublincore']).query({
    query,
  }).then((rep) => {
    console.log(colors.gray(`Secondary Count: ${rep.resultsCount}`));
    if (docs.resultsCount !== rep.resultsCount) {
      console.log(colors.red(`Secondary server doesn't match primary ${docs.resultsCount}`));
    } else {
      console.log(colors.green('Secondary and primary have the same number of documents.'));
    }
  });
});

connect.primary.http({
  method: 'POST',
  url: `${connect.Config.get('primary')}/nuxeo/site/es/nuxeo/_search`,
  body: '{ "query": { "match_all":{} }, "size": 0}',
}).then((docs) => {
  console.log(colors.gray(`Primary index count: ${docs.hits.total}`));

  connect.secondary.http({
    method: 'POST',
    url: `${connect.Config.get('secondary')}/nuxeo/site/es/nuxeo/_search`,
    body: '{ "query": { "match_all":{} }, "size": 0}',
  }).then((rep) => {
    console.log(colors.gray(`Secondary index count: ${rep.hits.total}`));
    if (docs.hits.total !== rep.hits.total) {
      console.log(colors.red(`Secondary index doesn't match primary ${docs.hits.total}`));
    } else {
      console.log(colors.green('Secondary and primary have the same number of index entries.'));
    }
  });
}).catch((err) => {
  console.log(colors.red(`oopsie: ${err}`));
});
