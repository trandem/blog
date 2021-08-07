# Simple RPC implement in java
Gần đây mọi người hay so sánh REST với RPC nên chọn công nghệ 
nào để truyền tải dữ liệu qua các server. Với REST chúng ta có
thể tìm thấy rất nhiều bài hướng dẫn trên internet. Các bài về tìm hiểu
cơ chế chạy, cách truyền dữ liệu thông qua body,... Những bài viết đó khiến
chúng ta quá quen thuộc với REST và nó không còn là hộp đen nữa. Trái ngược với
điều trên thì RPC lại không nhiều bài viết hướng dẫn mọi người implement, mọi 
người thường tìm thấy cách sử dụng của một số framework như gRPC, thrift,... và các
bài so sánh hiệu năng viết RPC nhanh hơn REST và thích hợp với truyền tải thông tin 
liên server hơn REST. Tại đây tôi có 1 implement nhỏ đơn giản về RPC hy vọng thông 
qua bài này mọi người sẽ không còn cảm thấy lạ với loại hình này và có thể giải thích
được tại sao nó lại thích hợp giữa các server.

Bài viết sẽ có các phần sau:
- transport
- serialize
# Transport
