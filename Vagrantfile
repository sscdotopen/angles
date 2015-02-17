VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.network "forwarded_port", guest: 3306, host: 3306

  config.vm.provider "virtualbox" do |v|
    v.memory = 2048
  end

  config.vm.provision :shell, path: "vagrant/mariadb.sh"
end
