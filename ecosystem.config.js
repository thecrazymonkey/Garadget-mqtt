module.exports = {
  /**
   * Application configuration section
   * http://pm2.keymetrics.io/docs/usage/application-declaration/
   */
  apps : [

    // First application
    {
      name      : 'mqtt2rest',
      script    : 'mqtt2rest.js',
      watch	: true,
      ignore_watch : ["node_modules"]
    }
  ]
};
