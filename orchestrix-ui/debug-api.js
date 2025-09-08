const axios = require('axios');

async function debugAPI() {
  try {
    // Get clouds
    const cloudsRes = await axios.get('http://localhost:8090/api/clouds?tenant=organization');
    const clouds = cloudsRes.data;
    console.log('☁️ Clouds:', clouds.length);
    
    if (clouds.length > 0) {
      const cloud = clouds[0];
      console.log('\n📊 First Cloud:');
      console.log('  - ID:', cloud.id);
      console.log('  - Name:', cloud.name);
      console.log('  - Partner ID:', cloud.partner?.id);
      console.log('  - Has regions:', !!cloud.regions);
      console.log('  - Regions count:', cloud.regions?.length || 0);
      
      if (cloud.regions && cloud.regions.length > 0) {
        const region = cloud.regions[0];
        console.log('\n📍 First Region:');
        console.log('  - ID:', region.id);
        console.log('  - Name:', region.name);
        console.log('  - Has cloud prop:', !!region.cloud);
        console.log('  - Has cloudId prop:', !!region.cloudId);
        console.log('  - Cloud ID (from prop):', region.cloud?.id || region.cloudId);
        console.log('  - Has AZs:', !!region.availabilityZones);
        console.log('  - AZs count:', region.availabilityZones?.length || 0);
        
        if (region.availabilityZones && region.availabilityZones.length > 0) {
          const az = region.availabilityZones[0];
          console.log('\n🏢 First AZ:');
          console.log('  - ID:', az.id);
          console.log('  - Name:', az.name);
          console.log('  - Has region prop:', !!az.region);
          console.log('  - Has regionId prop:', !!az.regionId);
          console.log('  - Has datacenters:', !!az.datacenters);
          console.log('  - Datacenters count:', az.datacenters?.length || 0);
          
          if (az.datacenters && az.datacenters.length > 0) {
            const dc = az.datacenters[0];
            console.log('\n🏭 First Datacenter:');
            console.log('  - ID:', dc.id);
            console.log('  - Name:', dc.name);
            console.log('  - Has availabilityZone prop:', !!dc.availabilityZone);
            console.log('  - Has availabilityZoneId prop:', !!dc.availabilityZoneId);
          }
        }
      }
    }
    
    // Check direct regions API
    console.log('\n🔍 Checking direct regions API...');
    try {
      const regionsRes = await axios.get('http://localhost:8090/api/regions?tenant=organization');
      console.log('Direct regions API:', regionsRes.status, '- Count:', regionsRes.data?.length || 0);
    } catch (e) {
      console.log('Direct regions API failed:', e.response?.status || e.message);
    }
    
    // Check direct AZs API
    console.log('\n🔍 Checking direct AZs API...');
    try {
      const azsRes = await axios.get('http://localhost:8090/api/availability-zones?tenant=organization');
      console.log('Direct AZs API:', azsRes.status, '- Count:', azsRes.data?.length || 0);
    } catch (e) {
      console.log('Direct AZs API failed:', e.response?.status || e.message);
    }
    
    // Check datacenters API
    console.log('\n🔍 Checking datacenters API...');
    const dcRes = await axios.get('http://localhost:8090/api/datacenters?tenant=organization');
    console.log('Datacenters API:', dcRes.status, '- Count:', dcRes.data?.length || 0);
    if (dcRes.data && dcRes.data.length > 0) {
      const dc = dcRes.data[0];
      console.log('First DC:', dc.id, dc.name, '- Has AZ:', !!dc.availabilityZone, '- Has AZ ID:', !!dc.availabilityZoneId);
    }
    
  } catch (error) {
    console.error('Error:', error.message);
  }
}

debugAPI();