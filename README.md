# registration

The `local.conf` file for this application can be found in the `dev` vault in 1Password. If you need access to 1Password, please ask Ryan for access. You can then move the `local.conf` file within the `registration/conf` directory.

Prior to running this app for the first time, you need to run `registration-service` run migrations on the local postgresql database. Instructions on how to run that application will be in the `registration-service` repo.

After the `registration_service_local` database has been created, you can run `registration`. To run, simply run `sbt "start -Dconfig.resource=local.conf"`.

