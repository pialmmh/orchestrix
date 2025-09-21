const axios = require('axios');

async function testQuery() {
  try {
    console.log('Testing partner query...');

    const query = {
      kind: "partner",
      criteria: { name: "telcobright" },
      page: { limit: 10, offset: 0 }
    };

    console.log('Query:', JSON.stringify(query, null, 2));

    const response = await axios.post('http://localhost:8090/api/stellar/query', query);

    console.log('Response status:', response.status);
    console.log('Response data type:', typeof response.data);
    console.log('Is array?', Array.isArray(response.data));

    if (response.data) {
      console.log('Response structure:');
      console.log('- Keys:', Object.keys(response.data));

      if (response.data.data) {
        console.log('- Has data field, type:', typeof response.data.data);
        console.log('- Is data array?', Array.isArray(response.data.data));
        if (Array.isArray(response.data.data)) {
          console.log('- Data length:', response.data.data.length);
          if (response.data.data.length > 0) {
            console.log('- First item:', response.data.data[0]);
          }
        }
      }

      console.log('\nFull response:', JSON.stringify(response.data, null, 2));
    }

  } catch (error) {
    console.error('Error:', error.message);
    if (error.response) {
      console.error('Response data:', error.response.data);
    }
  }
}

testQuery();