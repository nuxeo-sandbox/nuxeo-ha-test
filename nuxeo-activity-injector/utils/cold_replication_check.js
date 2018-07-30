const connect = require('./connect.js');
const colors = require('colors/safe');
const fs = require('fs');
const readline = require('readline');
const Stream = require('stream');

const Queue = require('better-queue');

let tick = 0,
  remaining = 0,
  blobok = 0,
  blobgone = 0,
  docok = 0,
  docgone = 0;

const q = new Queue((task, done) => {
  const parts = task.split(',');
  connect.primary.repository().fetch(parts[0]).then((doc) => {
    if (doc) {
      docok += 1;
    }
    connect.primary.operation('Blob.VerifyBinaryHash')
      .param('digest', parts[1])
      .execute().then((result) => {
        if (result.value === parts[1]) {
          blobok += 1;
        } else {
          console.log(colors.red(`noblob:${parts[1]}`));
          blobgone += 1;
        }
        done();
      })
      .catch(() => {
        console.log(colors.red(`blobfail:${parts[1]}`));
        blobgone += 1;
        done();
      });
  }).catch((error) => {
    docgone += 1;
    console.log(colors.red(`nodoc:${parts[0]},${error}`));
    connect.primary.operation('Blob.VerifyBinaryHash')
      .param('digest', parts[1])
      .execute().then((result) => {
        if (result.value === parts[1]) {
          blobok += 1;
        } else {
          console.log(colors.red(`noblob:${parts[1]}`));
          blobgone += 1;
        }
        done();
      })
      .catch(() => {
        console.log(colors.red(`blobfail:${parts[1]}`));
        blobgone += 1;
        done();
      });
  });
}, {
  concurrent: 10,
});

q.on('task_queued', () => {
  remaining += 1;
});
q.on('task_finish', () => {
  remaining -= 1;
  tick += 1;
  if ((tick % 100) === 0) {
    console.log(colors.gray(`${tick}: ${docok}/${docgone}; ${blobok}/${blobgone} -${remaining}`));
  }
});
q.on('drain', () => {
  console.log(colors.green(`${tick}: ${docok}/${docgone}; ${blobok}/${blobgone}`));
});

let currentLine = null;

function processFile(inputFile) {
  const instream = fs.createReadStream(inputFile);
  const outstream = new(Stream)();
  const rl = readline.createInterface(instream, outstream);

  rl.on('line', (line) => {
    if (currentLine) {
      q.push(line);
    }
    currentLine = line;
  });
}
processFile(connect.Config.get('outputFile'));
