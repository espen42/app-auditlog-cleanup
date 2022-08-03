var nodeLib = require('/lib/xp/node');

//var TARGET_ID = "58b48a7d-9ac5-4f37-b3a5-0dd1629fff88"        // <-- Actual target on AS prod
var TARGET_ID = "bce9cf17-0894-46a4-be23-95cbf2dc8992"          // <-- Exists in my local repo

log.info("Handling node " + TARGET_ID);

try {
    var repo = nodeLib.connect({
        repoId: "system.auditlog",
        branch: "master"
    });

    var exists = repo.exists(TARGET_ID);

    if (!exists || exists.length < 1) {
        throw Error("Node " + TARGET_ID + " doesn't exist.");

    } else {
        log.info("Node " + TARGET_ID + " exists. Deleting it...");
        var result = repo.delete(TARGET_ID);
        log.info("Removal result: " + JSON.stringify(result));
    }

} catch (e) {
    log.error(e);
}


// var adminToolsBean = __.newBean(
//     'handler.CleanUpHandler'
// );
//
// adminToolsBean.run('2014-09-25T10:00:00.00Z');

