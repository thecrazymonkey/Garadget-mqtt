const Joi = require('joi');
const Boom = require('boom');
const Extend = require('extend');

module.exports = function validate(schema, options) {
  options = options || {};

  return function validateRequest(req, res, next) {
    let toValidate = {};
    /* istanbul ignore if */
    if (!schema) {
      return next();
    }

    ['params', 'body', 'query'].forEach(function (key) {
      if (schema[key]) {
        toValidate[key] = req[key];
      }
    });
    
    return Joi.validate(toValidate, schema, options, onValidationComplete);

    function onValidationComplete(err, validated) {
      if (err) {
        return next(Boom.badRequest(err.message, err.details));
      }

      // copy the validated data to the req object
      Extend(req, validated);

      return next();
    }
  }
}