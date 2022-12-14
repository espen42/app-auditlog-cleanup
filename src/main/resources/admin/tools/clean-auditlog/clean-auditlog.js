var thymeleaf = require('/lib/thymeleaf');

var view = resolve('./clean-auditlog.html');

var MY_URL = '/admin/tool/' + app.name + '/clean-auditlog'

exports.get = function (req) {
    return {
        body: thymeleaf.render(view, {
            started: false,
            myUrl: MY_URL
        })
    }
};

exports.post = function (req) {
    var adminToolsBean = __.newBean('handler.CleanUpHandler');
    adminToolsBean.run(req.params.until);

    return {
        body: thymeleaf.render(view, {
            started: true
        })
    }
};
