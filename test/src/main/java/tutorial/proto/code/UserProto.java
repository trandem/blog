// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: userservice.proto

package tutorial.proto.code;

public final class UserProto {
  private UserProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_UserRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_UserRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_UserReply_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_UserReply_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n\021userservice.proto\"\'\n\013UserRequest\022\n\n\002ID" +
      "\030\001 \001(\005\022\014\n\004name\030\002 \001(\t\"\034\n\tUserReply\022\017\n\007mes" +
      "sage\030\001 \001(\t2,\n\004User\022$\n\006invoke\022\014.UserReque" +
      "st\032\n.UserReply\"\000B\"\n\023tutorial.proto.codeB" +
      "\tUserProtoP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_UserRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_UserRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_UserRequest_descriptor,
        new String[] { "ID", "Name", });
    internal_static_UserReply_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_UserReply_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_UserReply_descriptor,
        new String[] { "Message", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
