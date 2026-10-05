const admin = require('firebase-admin');
const serviceAccount = require('../../My Countdowns App/firebase-service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

admin.auth().getUserByEmail('info@automatedbusinessprocesses.com')
  .then((userRecord) => {
    console.log('Successfully fetched user data:', userRecord.toJSON());
    process.exit(0);
  })
  .catch((error) => {
    console.log('Error fetching user data:', error);
    process.exit(1);
  });
