const axios = require('axios');

async function testInfrastructureLoad() {
  try {
    console.log('Testing infrastructure load from API...\n');

    // Test direct API call - infrastructure is built from cloud entities
    const apiResponse = await axios.post('http://localhost:8090/api/stellar/query', {
      kind: 'cloud',
      options: {
        pageSize: 50
      }
    });

    console.log('API Response Status:', apiResponse.status);
    console.log('Infrastructure count:', apiResponse.data?.length || 0);

    if (apiResponse.data && apiResponse.data.length > 0) {
      console.log('✅ Infrastructure data loaded successfully!');
      console.log('\nFirst infrastructure item:');
      console.log(JSON.stringify(apiResponse.data[0], null, 2));
    } else {
      console.log('⚠️ No infrastructure data returned from API');
    }

  } catch (error) {
    console.error('❌ Error loading infrastructure:', error.message);
    if (error.response) {
      console.error('Response status:', error.response.status);
      console.error('Response data:', error.response.data);
    }
  }
}

testInfrastructureLoad();