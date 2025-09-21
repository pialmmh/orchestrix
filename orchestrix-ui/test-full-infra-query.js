const axios = require('axios');

async function testFullInfraQuery() {
  try {
    console.log('Testing full infrastructure query for Telcobright...\n');

    const query = {
      kind: "partner",
      criteria: { name: "telcobright" },
      page: { limit: 10, offset: 0 },
      include: [
        {
          kind: "cloud",
          page: { limit: 20, offset: 0 },
          include: [
            {
              kind: "region",
              page: { limit: 50, offset: 0 },
              include: [
                {
                  kind: "availabilityzone",
                  page: { limit: 100, offset: 0 },
                  include: [
                    {
                      kind: "datacenter",
                      page: { limit: 200, offset: 0 },
                      include: [
                        {
                          kind: "compute",
                          page: { limit: 500, offset: 0 }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    };

    console.log('Query:', JSON.stringify(query, null, 2));

    const response = await axios.post('http://localhost:8090/api/stellar/query', query);

    console.log('\nResponse status:', response.status);
    console.log('Has data?', !!response.data);

    if (response.data && response.data.data && Array.isArray(response.data.data)) {
      console.log('Partners count:', response.data.data.length);

      if (response.data.data.length > 0) {
        const partner = response.data.data[0];
        console.log('\nPartner:', partner.name);

        if (partner.clouds) {
          console.log('Clouds count:', partner.clouds.length);
          partner.clouds.forEach(cloud => {
            console.log(`  - Cloud: ${cloud.name} (${cloud.provider})`);

            if (cloud.regions) {
              console.log(`    Regions: ${cloud.regions.length}`);
              cloud.regions.forEach(region => {
                console.log(`      - ${region.name}`);

                if (region.availabilityzones) {
                  console.log(`        AZs: ${region.availabilityzones.length}`);
                  region.availabilityzones.forEach(az => {
                    console.log(`          - ${az.name}`);

                    if (az.datacenters) {
                      console.log(`            Datacenters: ${az.datacenters.length}`);
                      az.datacenters.forEach(dc => {
                        console.log(`              - ${dc.name}`);

                        if (dc.computes) {
                          console.log(`                Computes: ${dc.computes.length}`);
                        }
                      });
                    }
                  });
                }
              });
            }
          });
        } else {
          console.log('No clouds found in response');
        }

        console.log('\nFull partner data structure:');
        console.log(JSON.stringify(partner, null, 2));
      }
    }

  } catch (error) {
    console.error('Error:', error.message);
    if (error.response) {
      console.error('Response status:', error.response.status);
      console.error('Response data:', JSON.stringify(error.response.data, null, 2));
    }
  }
}

testFullInfraQuery();