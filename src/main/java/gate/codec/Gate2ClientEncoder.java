package gate.codec;


import gate.base.domain.ChannelData;
import gate.base.domain.SocketData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
/**
 * 编码器 将对象 编码成字节数组  --->目的地是终端硬件
 * @author BriansPC
 *
 */
public class Gate2ClientEncoder extends MessageToByteEncoder<ChannelData>{

	@Override
	protected void encode(ChannelHandlerContext ctx, ChannelData msg, ByteBuf out) throws Exception {
		SocketData data = msg.getSocketData();

		ByteBuf buf = Unpooled.directBuffer();
		buf.writeByte(data.getHeader());
		buf.writeBytes(data.getLenArea());
		buf.writeBytes(data.getContent());
		buf.writeByte(data.getEnd());
		out.writeBytes(buf);
	}
}
