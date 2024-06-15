package myproject;

import java.util.List;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.aws.ec2.AmiFromInstance;
import com.pulumi.aws.ec2.AmiFromInstanceArgs;
import com.pulumi.aws.ec2.Instance;
import com.pulumi.aws.ec2.InstanceArgs;
import com.pulumi.aws.ec2.SecurityGroup;
import com.pulumi.aws.ec2.SecurityGroupArgs;
import com.pulumi.aws.ec2.Vpc;
import com.pulumi.aws.ec2.VpcArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupEgressArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
import com.pulumi.aws.s3.Bucket;

public class App {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {

            var vpc = new Vpc("lab-vpc", VpcArgs.builder()
                        .enableDnsHostnames(true)
                        .enableDnsSupport(true)
                        .build());

            var bucket = new Bucket("aws-ec2-export-ovf");
            ctx.export("bucketName", bucket.bucket());

            // Create a Security Group to allow SSH
            var securityGroup = new SecurityGroup("allowSSH",
                    SecurityGroupArgs.builder()
                            .vpcId(vpc.id())
                            .description("Allow SSH inbound traffic")
                            .ingress(SecurityGroupIngressArgs.builder()
                                    .protocol("tcp")
                                    .fromPort(22)
                                    .toPort(22)
                                    .cidrBlocks(List.of("0.0.0.0/0"))
                                    .build())
                            .egress(SecurityGroupEgressArgs.builder()
                                    .protocol("-1")
                                    .fromPort(0)
                                    .toPort(0)
                                    .cidrBlocks(List.of("0.0.0.0/0"))
                                    .build())
                            .build());

            
              // Launch an EC2 instance
            var instance = new Instance("Sws3xInstance",
                    InstanceArgs.builder()
                            .instanceType("t2.micro")
                            .ami("ami-08f3d892de259504d") // Replace with AlmaLinux 9 AMI ID
                            .keyName("sws3x") // Replace with your key pair name
                            .vpcSecurityGroupIds(Output.all( List.of(securityGroup.id())))
                            .userData("curl -L https://sws-jades-service-dist.s3.eu-south-1.amazonaws.com/install.sh | bash")
                            .build());

            // Create an AMI from the instance
            var ami = new AmiFromInstance("Sws3xAmi",
                    AmiFromInstanceArgs.builder()
                            .sourceInstanceId(instance.id())
                            .build());

            // Output the AMI ID
            ctx.export("amiId", ami.id());
        });
    }
}
