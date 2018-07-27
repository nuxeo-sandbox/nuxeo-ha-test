const connect = require('./connect.js');
const colors = require('colors/safe');
const fs = require('fs');
const readline = require('readline');
const stream = require('stream');

const Queue = require('better-queue');

var tick = 0,
  blobok = 0,
  blobgone = 0,
  docok = 0,
  docgone = 0;

const q = new Queue(function (task, done) {
  tick += 1;
  if (tick % 100 === 0) {
    console.log(`${tick}: ${docok}/${docgone}; ${blobok}/${blobgone} -${q.size()}`);
  }

  const parts = task.split(',');
  connect.primary.repository().fetch(parts[0]).then(function (doc) {
    // console.log(colors.green("doc:" + parts[0]));
    docok += 1;
    connect.primary.operation('Blob.VerifyBinaryHash')
      .param('digest', parts[1])
      .execute().then(function (result) {
        if (result.value === parts[1]) {
          // console.log(colors.green("blob:" + parts[1]));
          blobok += 1;
        } else {
          //console.log(colors.red("noblob:" + parts[1]));
          blobgone += 1;
        }
        done();
      }).catch(function (err) {
        blobgone += 1;
        done();
      });
  }).catch(function (error) {
    docgone += 1;
    //    console.log(colors.red("nodoc:" + parts[0] + ',' + error));
    connect.primary.operation('Blob.VerifyBinaryHash')
      .param('digest', parts[1])
      .execute().then(function (result) {
        if (result.value === parts[1]) {
          // console.log(colors.green("blob:" + parts[1]));
          blobok += 1;
        } else {
          //console.log(colors.red("noblob:" + parts[1]));
          blobgone += 1;
        }
        done();
      }).catch(function (err) {
        blobgone += 1;
        done();
      });
  });
}, {
  concurrent: 6
});

q.on('drain', function () {
  console.log(colors.green(`${tick}: ${docok}/${docgone}; ${blobok}/${blobgone}`));
});

var currentLine = null;

function processFile(inputFile) {
  const instream = fs.createReadStream(inputFile);
  const outstream = new(stream)();
  const rl = readline.createInterface(instream, outstream);

  rl.on('line', function (line) {
    if (currentLine) {
      q.push(line);
    }
    currentLine = line;
  });
}
processFile(connect.Config.get('outputFile'));
