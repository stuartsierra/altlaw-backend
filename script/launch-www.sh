#!/bin/bash

S3_BUCKET=altlaw.org
AMI_NAME=www2
HADOOP_VERSION=0.18.4-dev

# Location of EC2 keys.  The default setting is probably OK if you set
# up EC2 following the Amazon Getting Started guide.
EC2_KEYDIR=`dirname "$EC2_PRIVATE_KEY"`

# The EC2 key name used to launch instances.  The default is the value
# used in the Amazon Getting Started guide.
KEY_NAME=stuart-altlaw

# Where your EC2 private key is stored (created when following the
# Amazon Getting Started guide).  You need to change this if you don't
# store this with your other EC2 keys.
PRIVATE_KEY_PATH=`echo "$EC2_KEYDIR"/"id_rsa-$KEY_NAME"`

# SSH options used when connecting to EC2 instances.
SSH_OPTS=`echo -i "$PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no -o ServerAliveInterval=30`

# The AMI image ID.
AMI_IMAGE=`ec2-describe-images -a | grep $S3_BUCKET | grep $AMI_NAME | grep available | awk '{print $2}'`


echo "Launching instance with image $AMI_IMAGE"
INSTANCE=`ec2-run-instances $AMI_IMAGE | grep INSTANCE | awk '{print $2}'`

echo "Waiting for instance $INSTANCE to boot ..."
while true; do
  printf "."
  # get private dns
  HOST=`ec2-describe-instances $INSTANCE | grep running | awk '{print $4}'`
  if [ ! -z $HOST ]; then
    echo "Started as $HOST"
    break;
  fi
  sleep 1
done


cat > /tmp/remote-prepare-www.sh <<EOF
#!/bin/bash

echo "export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID" >> ~/.bashrc
echo "export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY" >> ~/.bashrc

ALTLAW_HOME=/mnt/altlaw
HADOOP_HOME=/mnt/hadoop

# install AltLaw
git clone --depth 1 git://github.com/lawcommons/altlaw-backend.git \$ALTLAW_HOME
cd \$ALTLAW_HOME
ant

# install Hadoop
cd /mnt
aws get altlaw.org/v4/hadoop-for-www.tar.gz hadoop.tar.gz
tar xzf hadoop.tar.gz
# Find the untarred Hadoop dir, rename it:
find . -maxdepth 1 -type d -name 'hadoop-*' -exec mv '{}' \$HADOOP_HOME \;
# Delete tar file:
rm /mnt/hadoop.tar.gz

# Prepare local Hadoop namenode, for s3dfs access:
cd \$HADOOP_HOME
bin/hadoop namenode -format

EOF


# Wait before trying to connect
sleep 10

# Copy .awssecret file:
scp $SSH_OPTS -p ~/.awssecret root@$HOST:

# Copy scripts:
scp $SSH_OPTS /tmp/remote-prepare-www.sh root@$HOST:

# Run the startup script:
ssh $SSH_OPTS root@$HOST "bash remote-prepare-www.sh"

# Login
ssh $SSH_OPTS root@$HOST


