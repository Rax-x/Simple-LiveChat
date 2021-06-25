const express = require('express');
const app = express();

const http = require('http');
const server = http.createServer(app);

const io = require('socket.io')(server, {
	cors:{
		origin: '*'
	}
});

const port = process.env.PORT || 3000;

io.on('connection', socket => {

	socket.on('user-join', nickname => {
		socket.nickname = nickname;
		socket.broadcast.emit('user-join', `${nickname} joined`);
	});

	socket.on('typing', (room) => {

		const payload = {
			nickname: socket.nickname,
			id: socket.id
		};

		if(room === ""){
			socket.broadcast.emit('user-typing', payload);
		}else{
			socket.broadcast.to(room).emit('user-typing', payload);
		}
	});

	socket.on('join-room', room => {
		socket.join(room);
		socket.broadcast.to(room).emit("user-join", `${socket.nickname} joined`);
	});

	socket.on('leave-room', room => {
		socket.broadcast.to(room).emit("leave-room", `${socket.nickname} left room`);
		socket.leave(room);
	});

	socket.on('stop-typing', (room) => {
		if(room === ""){
			socket.broadcast.emit('user-stop-typing', socket.id);
		}else{
			socket.broadcast.to(room).emit('user-stop-typing', socket.id);
		}
	});

	socket.on('disconnect', () => {
		socket.broadcast.emit('user-disconnect', `${socket.nickname} left chat`);
	});

	socket.on('chat-message', (message, room) => {
		if(room === ""){
			socket.broadcast.emit('chat-message', message);
		}else{
			socket.broadcast.to(room).emit('chat-message', message);
		}
	});
});

app.get('/', (req, res) => {
	res.json({
		status: 200,
		message: `LiveChat server is online on port ${port}`
	});
});

server.listen(port, () => {
	console.log(`Server listening on port ${port}`);
});
