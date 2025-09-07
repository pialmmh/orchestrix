with orchestrix we will perform various deplaoyment task like jenkins, pullumi over
a connectivity adapter. all our source codes will be java for now.  

interface:
    ConnectivityAdapter
        impls:
            SSH
            [will add in future]
flow requirements:
    for testing we want this: orchestrix>connect to a compute instance defined in the db> run ssh script 
through expectJ. the script should be separate into groups of significant tasks, transient retries
and printing verbose and useful output through web socket to the react client, user can see the job running
with progress (virtual progress by groupign of tasks/steps, not exact) and 

(for simplicity, let's allow keeping secrets in the db e.g ssh username and password, 
we will make it more secure later). 

and for prototyping, we will connect to my ubuntu pc (mustafa-pc), which will be configured
as a compute node in orchestrix database. 

ui and entitiy modeling requirements:
    - runner job inventory like jenkins, pullimi, octopus
    - define job catagory and job (use your knowledge e.g. deployment, 
        container installation, mysql install, AWS service install. use some real world seed data in db).
        properly design the db so that the we can define new catagories and jobs. also, think and apply
        more layers other than catagories and jobs if we need to organize jobs better, like our infrastructures. 
    - user can select the job on the left. job output section with a console that immediately connects 
        in the background through a websocket client to the backend spring application for exchanaging 
        commands and getting console output of jobs actually running in the server. use lazy-log or think if 
        you use a better alternative with clearing logs, keeping no of lines features. 
    - user is able to search for jobs and job status, success/fail/error. also can retry.
    - user must select one or multiple target compute nodes before starting a job. the job will be run 
    in all nodes in parallel. user can select Tabs above the log viewer section to select which compute to see. 