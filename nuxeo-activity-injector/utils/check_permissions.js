const connect = require('./connect.js');
const colors = require('colors/safe');


var onWhichDoc = '/default-domain/workspaces';

connect.primary.repository()
  // We add the ACLs enricher to obtain current permissions on the doc
  .enricher('document', 'acls')
  // Then fetch the document
  .fetch(onWhichDoc)
  .then(function (doc) {
    console.log('Permissions defined on ' + doc.title + ':');
    console.log('Document: ' + JSON.stringify(doc, null, 2));
    for (var indexAcls = 0; indexAcls < doc.contextParameters.acls.length; indexAcls++) {
      console.log(doc.contextParameters.acls[indexAcls]);
    }
  })
  .catch(function (error) {
    console.log('Apologies, an error occurred while retrieving the permissions.');
    console.log(error);
  });
