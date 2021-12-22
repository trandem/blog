# Distributed cached
**Caching** là một kỹ thuật quen thuộc với tất cả anh em lập trình viên,lợi ích của việc sử 
dụng **caching** để tăng tải hệ thống thì chắc ai cũng biết.

Hiện nay khi xây dụng phần mềm hiện nay thì chúng ta sẽ không **deploy** riêng lẻ một **service**
mà sẽ là **deploy** nhiều service khác nhau và mỗi **service** cũng có thể có các **slave**.

Vậy chúng ta cần xây dựng một phương án hợp lý để xây dựng **caching** giữa các **service**, khi một **service** thay đổi
cache tất cả **service** sẽ được cập nhật nội dung mới này. Sẽ rất dễ dàng tìm được đó là sử dụng một **in-mem database** như : redis, mem cache,...

Trong các phần mềm mình đã tiếp xúc và xây dựng như : **antispam**, **trading**, ... Các hệ thống 
này thường yêu cầu độ trễ thấp cũng như có tải rất cao, vậy nên phương án dùng **in-mem database** không
phải là giải pháp tối ưu cho lắm. 

Mặc dù các **database** này có tốc độ rất cao nhưng nếu mình **query** cũng sẽ rất tốn chi phí về : **connection**,
 **context switching**, mạng. Đặc biệt với **redis** nếu mình không sử dụng thư viện [lettuce](https://lettuce.io/) 
 để giao tiếp **acsync** thì tài nguyên hệ thống bỏ ra sẽ là rất lớn khi tải cao. Tại đây có thể các bạn chưa trải qua cảm giác hệ thống mình
quá phụ thuộc quá nhiều các **in-mem database** đến khi **database** chậm thì khó có thể nào đưa ra được
 cách tối ưu hợp lý, bọn mình đã từng tăng gấp 3 số lượng master của 1 cụm **redis-cluster** từ 3 lên 9 mà tại thời điểm cao 
 tải vẫn không phục vụ được. Vì **redis-cluster** sẽ lưu dữ liệu dữa trên các **16384 slot** nếu tăng lên 9 thì số lượng
 **slot** sẽ được chia đều cho các **master** theo lý thuyết thì nó có thể mạnh như 9 **server redis**. Bài này mình không
 chia sẻ nhiều về **redis** nhưng nếu các bạn quan tâm thì mình sẽ ra bài này.

Hiện tại hệ thống của công ty mình đang sử dụng **distributed cache** mà không phụ thuộc vào **im-mem database** 
tất cả dữ liệu của được **cache** trên **RAM** để tăng hiệu năng tổng thể của toàn bộ hệ thống. Tất nhiên hệ thống
**distributed cache** này sẽ tự động **update** khi có một **service** thay đổi nội dung của **cache** (thường sẽ là do
admin thay đổi dữ liệu cache để turning hệ thống hoặc các job định kỳ)

Tại bài này mình sẽ chia sẽ cho các bạn kiến thức để xây dựng một **distributed cache** để nếu một ngày **in-mem database**
của bạn không hoạt động như ý muốn thì coi đây là một giải pháp.

 