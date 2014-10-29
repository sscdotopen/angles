VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.network "forwarded_port", guest: 3306, host: 13306
  config.vm.synced_folder "vagrant_data", "/vagrant_data"
  config.vm.provision :shell, path: "mariadb.sh"
end
